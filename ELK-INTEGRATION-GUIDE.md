# ELK Stack Integration Guide

이 가이드는 Spring Boot 마이크로서비스에서 ELK (Elasticsearch, Logstash, Kibana) 스택과 Filebeat를 통한 로그 수집 파이프라인을 구축하는 방법을 설명합니다.

## 🏗️ 아키텍처 개요

```
Spring Boot App (Container) → Filebeat → Logstash → Elasticsearch → Kibana
```

### 핵심 구성 요소
- **Filebeat**: Docker 컨테이너 로그 수집
- **Logstash**: 로그 파싱 및 변환 처리
- **Elasticsearch**: 로그 데이터 저장 및 인덱싱
- **Kibana**: 로그 데이터 시각화 및 검색

## 📋 사전 요구사항

### 1. Spring Boot 애플리케이션 설정

#### Logback JSON 설정 (`src/main/resources/logback-spring.xml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <springProperty scope="context" name="springAppName" source="spring.application.name" defaultValue="your-service-name"/>

    <appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <customFields>{"service":"${springAppName:-}","pid":"${PID:-}"}</customFields>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <level>level</level>
                <thread>thread</thread>
                <logger>logger</logger>
                <message>message</message>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE_JSON"/>
    </root>

    <logger name="your.package" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE_JSON"/>
    </logger>
</configuration>
```

#### 필수 의존성 (`build.gradle`)
```gradle
dependencies {
    implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
    // ... 기타 의존성
}
```

### 2. ELK 스택 인증 정보 (.env 파일)
```bash
# ELK Stack Credentials
ELASTIC_USERNAME=elastic
ELASTIC_PASSWORD=your_elasticsearch_password
```

## 🔧 Filebeat 설정

### 1. Filebeat 설정 파일 (`filebeat/filebeat.yml`)

```yaml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  
  # JSON 파싱 설정  
  json.keys_under_root: true
  json.add_error_key: true
  json.message_key: log

# 전역 프로세서 설정
processors:
  - add_docker_metadata:
      host: "unix:///var/run/docker.sock"
  
  # 특정 서비스만 필터링 (선택사항)
  - drop_event:
      when:
        not:
          contains:
            container.name: "your-service-container-name"

# Logstash 출력 설정
output.logstash:
  hosts: ["localhost:5044"]

# 로그 레벨 설정
logging.level: info
logging.to_files: true
logging.files:
  path: /usr/share/filebeat/logs
  name: filebeat
  keepfiles: 7
  permissions: 0644
```

### 2. Filebeat 컨테이너 실행
```bash
docker run -d \
  --name your-filebeat \
  --network host \
  --user root \
  -v $(pwd)/filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro \
  -v /var/lib/docker/containers:/var/lib/docker/containers:ro \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  docker.elastic.co/beats/filebeat:7.17.0
```

## 🔄 Logstash 파이프라인 설정

### 1. Logstash 설정 파일 (`logstash/pipeline/your-service.conf`)

```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  # JSON 파싱
  if [message] {
    json {
      source => "message"
      target => "app"
    }
  }
  
  # 컨테이너 정보 처리
  if [container][name] {
    mutate {
      add_field => { "service_name" => "%{[container][name]}" }
    }
    
    # Spring Boot 로그 레벨 정규화
    if [app][level] {
      mutate {
        uppercase => [ "[app][level]" ]
      }
    }
    
    # 에러 로그 태깅
    if [app][level] == "ERROR" {
      mutate {
        add_tag => [ "error" ]
      }
    }
    
    # 스택 트레이스 태깅
    if [app][stack_trace] and [app][stack_trace] != "" {
      mutate {
        add_tag => [ "exception" ]
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    user => "${ELASTIC_USERNAME}"
    password => "${ELASTIC_PASSWORD}"
    index => "your-service-logs-%{+YYYY.MM.dd}"
    manage_template => true
    template_name => "your-service-logs"
    template_pattern => "your-service-logs-*"
    template => {
      "index_patterns" => ["your-service-logs-*"]
      "settings" => {
        "number_of_shards" => 1
        "number_of_replicas" => 0
        "index.refresh_interval" => "5s"
      }
      "mappings" => {
        "properties" => {
          "@timestamp" => { "type" => "date" }
          "service_name" => { "type" => "keyword" }
          "app" => {
            "properties" => {
              "service" => { "type" => "keyword" }
              "level" => { "type" => "keyword" }
              "logger" => { "type" => "keyword" }
              "thread" => { "type" => "keyword" }
              "message" => { 
                "type" => "text",
                "analyzer" => "standard"
              }
              "stack_trace" => { 
                "type" => "text",
                "analyzer" => "keyword"
              }
              "pid" => { "type" => "keyword" }
            }
          }
          "container" => {
            "properties" => {
              "name" => { "type" => "keyword" }
              "image" => {
                "properties" => {
                  "name" => { "type" => "keyword" }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

## 🚀 배포 가이드

### Local Docker 환경

#### 1. Docker Compose 설정 (`docker-compose.yml`)
```yaml
version: '3.8'

services:
  your-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: your-service-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_APPLICATION_NAME=your-service
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    networks:
      - elk-network

  filebeat:
    image: docker.elastic.co/beats/filebeat:7.17.0
    user: root
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    depends_on:
      - logstash
    networks:
      - elk-network

networks:
  elk-network:
    external: true
```

#### 2. 실행 명령어
```bash
# ELK 스택 시작 (기존 ELK가 없는 경우)
docker-compose -f elk-stack.yml up -d

# 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f your-service
docker-compose logs -f filebeat
```

### AWS EKS 환경

#### 📍 EKS 네트워킹 개요
EKS에서는 다음과 같은 서비스명을 사용합니다:
- **동일 네임스페이스**: `service-name:port`
- **다른 네임스페이스**: `service-name.namespace.svc.cluster.local:port`
- **External/LoadBalancer**: 실제 도메인 또는 IP 주소

#### 1. 네임스페이스 생성 (`k8s/00-namespaces.yaml`)
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: logging
---
apiVersion: v1
kind: Namespace
metadata:
  name: microservices
```

#### 2. Elasticsearch 서비스 (`k8s/elasticsearch-service.yaml`)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: elasticsearch-service
  namespace: logging
spec:
  ports:
  - name: http
    port: 9200
    targetPort: 9200
  - name: transport
    port: 9300
    targetPort: 9300
  selector:
    app: elasticsearch
  type: ClusterIP
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: elasticsearch
  namespace: logging
spec:
  serviceName: elasticsearch-service
  replicas: 1
  selector:
    matchLabels:
      app: elasticsearch
  template:
    metadata:
      labels:
        app: elasticsearch
    spec:
      containers:
      - name: elasticsearch
        image: docker.elastic.co/elasticsearch/elasticsearch:7.17.0
        env:
        - name: discovery.type
          value: single-node
        - name: ES_JAVA_OPTS
          value: "-Xms512m -Xmx512m"
        - name: ELASTIC_PASSWORD
          valueFrom:
            secretKeyRef:
              name: elasticsearch-credentials
              key: password
        ports:
        - containerPort: 9200
        - containerPort: 9300
        resources:
          limits:
            memory: 1Gi
          requests:
            memory: 512Mi
```

#### 3. Logstash 서비스 (`k8s/logstash-service.yaml`)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: logstash-service
  namespace: logging
spec:
  ports:
  - name: beats
    port: 5044
    targetPort: 5044
  - name: tcp
    port: 50000
    targetPort: 50000
  selector:
    app: logstash
  type: ClusterIP
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: logstash
  namespace: logging
spec:
  replicas: 1
  selector:
    matchLabels:
      app: logstash
  template:
    metadata:
      labels:
        app: logstash
    spec:
      containers:
      - name: logstash
        image: docker.elastic.co/logstash/logstash:7.17.0
        env:
        - name: LS_JAVA_OPTS
          value: "-Xms256m -Xmx512m"
        - name: ELASTICSEARCH_HOST
          value: "elasticsearch-service.logging.svc.cluster.local"
        - name: ELASTICSEARCH_PORT
          value: "9200"
        - name: ELASTIC_USERNAME
          valueFrom:
            secretKeyRef:
              name: elasticsearch-credentials
              key: username
        - name: ELASTIC_PASSWORD
          valueFrom:
            secretKeyRef:
              name: elasticsearch-credentials
              key: password
        ports:
        - containerPort: 5044
        - containerPort: 50000
        volumeMounts:
        - name: logstash-config
          mountPath: /usr/share/logstash/pipeline/
        resources:
          limits:
            memory: 1Gi
          requests:
            memory: 512Mi
      volumes:
      - name: logstash-config
        configMap:
          name: logstash-config
```

#### 4. Logstash 파이프라인 ConfigMap (`k8s/logstash-config.yaml`)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: logstash-config
  namespace: logging
data:
  logstash.conf: |-
    input {
      beats {
        port => 5044
      }
    }

    filter {
      # JSON 파싱
      if [message] {
        json {
          source => "message"
          target => "app"
        }
      }
      
      # Kubernetes 메타데이터 처리
      if [kubernetes][container][name] {
        mutate {
          add_field => { "service_name" => "%{[kubernetes][container][name]}" }
          add_field => { "namespace" => "%{[kubernetes][namespace]}" }
          add_field => { "pod_name" => "%{[kubernetes][pod][name]}" }
        }
        
        # Spring Boot 로그 레벨 정규화
        if [app][level] {
          mutate {
            uppercase => [ "[app][level]" ]
          }
        }
        
        # 에러 로그 태깅
        if [app][level] == "ERROR" {
          mutate {
            add_tag => [ "error" ]
          }
        }
      }
    }

    output {
      elasticsearch {
        hosts => ["${ELASTICSEARCH_HOST}:${ELASTICSEARCH_PORT}"]
        user => "${ELASTIC_USERNAME}"
        password => "${ELASTIC_PASSWORD}"
        index => "microservices-logs-%{[namespace]}-%{+YYYY.MM.dd}"
        manage_template => true
        template_name => "microservices-logs"
        template_pattern => "microservices-logs-*"
      }
    }
```

#### 5. Filebeat DaemonSet (`k8s/filebeat-daemonset.yaml`)
```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: filebeat
  namespace: logging
spec:
  selector:
    matchLabels:
      name: filebeat
  template:
    metadata:
      labels:
        name: filebeat
    spec:
      serviceAccountName: filebeat
      terminationGracePeriodSeconds: 30
      containers:
      - name: filebeat
        image: docker.elastic.co/beats/filebeat:7.17.0
        args: [
          "-c", "/etc/filebeat.yml",
          "-e"
        ]
        env:
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        - name: LOGSTASH_HOST
          value: "logstash-service.logging.svc.cluster.local"
        - name: LOGSTASH_PORT
          value: "5044"
        securityContext:
          runAsUser: 0
        resources:
          limits:
            memory: 200Mi
          requests:
            cpu: 100m
            memory: 100Mi
        volumeMounts:
        - name: config
          mountPath: /etc/filebeat.yml
          readOnly: true
          subPath: filebeat.yml
        - name: varlog
          mountPath: /var/log
          readOnly: true
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
        - name: dockersock
          mountPath: /var/run/docker.sock
          readOnly: true
      volumes:
      - name: config
        configMap:
          defaultMode: 0644
          name: filebeat-config
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      - name: dockersock
        hostPath:
          path: /var/run/docker.sock
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: filebeat
  namespace: logging
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: filebeat
subjects:
- kind: ServiceAccount
  name: filebeat
  namespace: logging
roleRef:
  kind: ClusterRole
  name: filebeat
  apiGroup: rbac.authorization.k8s.io
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: filebeat
  labels:
    app: filebeat
rules:
- apiGroups: [""]
  resources:
  - nodes
  - namespaces
  - pods
  verbs:
  - get
  - list
  - watch
```

#### 6. Filebeat ConfigMap (`k8s/filebeat-config.yaml`)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: filebeat-config
  namespace: logging
data:
  filebeat.yml: |-
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*.log
      processors:
        - add_kubernetes_metadata:
            host: ${NODE_NAME}
            matchers:
            - logs_path:
                logs_path: "/var/log/containers/"
        # 특정 네임스페이스만 수집 (선택사항)
        - drop_event:
            when:
              not:
                equals:
                  kubernetes.namespace: "microservices"
    
    output.logstash:
      hosts: ["${LOGSTASH_HOST}:${LOGSTASH_PORT}"]
    
    logging.level: info
    logging.to_files: true
    logging.files:
      path: /var/log/filebeat
      name: filebeat
      keepfiles: 7
```

#### 7. Secrets 관리 (`k8s/elasticsearch-credentials.yaml`)
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: elasticsearch-credentials
  namespace: logging
type: Opaque
data:
  username: ZWxhc3RpYw==  # elastic (base64)
  password: WW91ckVsYXN0aWNQYXNzd29yZA==  # YourElasticPassword (base64)
```

#### 8. Kibana 서비스 (`k8s/kibana-service.yaml`)
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kibana-service
  namespace: logging
spec:
  ports:
  - port: 5601
    targetPort: 5601
  selector:
    app: kibana
  type: LoadBalancer  # 또는 ClusterIP + Ingress
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kibana
  namespace: logging
spec:
  replicas: 1
  selector:
    matchLabels:
      app: kibana
  template:
    metadata:
      labels:
        app: kibana
    spec:
      containers:
      - name: kibana
        image: docker.elastic.co/kibana/kibana:7.17.0
        env:
        - name: ELASTICSEARCH_HOSTS
          value: "http://elasticsearch-service.logging.svc.cluster.local:9200"
        - name: ELASTICSEARCH_USERNAME
          valueFrom:
            secretKeyRef:
              name: elasticsearch-credentials
              key: username
        - name: ELASTICSEARCH_PASSWORD
          valueFrom:
            secretKeyRef:
              name: elasticsearch-credentials
              key: password
        ports:
        - containerPort: 5601
        resources:
          limits:
            memory: 1Gi
          requests:
            memory: 512Mi
```

#### 9. EKS 배포 명령어
```bash
# 1. 네임스페이스 생성
kubectl apply -f k8s/00-namespaces.yaml

# 2. Secrets 생성
kubectl apply -f k8s/elasticsearch-credentials.yaml

# 3. Elasticsearch 배포
kubectl apply -f k8s/elasticsearch-service.yaml

# 4. Logstash 배포
kubectl apply -f k8s/logstash-config.yaml
kubectl apply -f k8s/logstash-service.yaml

# 5. Filebeat 배포
kubectl apply -f k8s/filebeat-config.yaml
kubectl apply -f k8s/filebeat-daemonset.yaml

# 6. Kibana 배포
kubectl apply -f k8s/kibana-service.yaml

# 7. 상태 확인
kubectl get pods -n logging
kubectl get svc -n logging

# 8. 로그 확인
kubectl logs -n logging -l name=filebeat
kubectl logs -n logging -l app=logstash
```

## 🔍 Kibana 대시보드 설정

### 1. Index Pattern 생성
1. Kibana 접속: `http://your-kibana:5601`
2. Management → Stack Management → Data Views
3. "Create data view" 클릭
4. 설정:
   - Name: `your-service-logs`
   - Index pattern: `your-service-logs-*`
   - Timestamp field: `@timestamp`

### 2. 로그 검색 및 필터링
```json
# 에러 로그만 조회
{
  "query": {
    "bool": {
      "must": [
        {"term": {"app.level": "ERROR"}},
        {"term": {"service_name": "your-service-app"}}
      ]
    }
  }
}

# 특정 시간대 로그 조회
{
  "query": {
    "bool": {
      "must": [
        {"range": {"@timestamp": {"gte": "now-1h"}}},
        {"term": {"service_name": "your-service-app"}}
      ]
    }
  }
}
```

## 🔧 트러블슈팅

### 일반적인 문제들

#### 1. Filebeat가 로그를 수집하지 못하는 경우
```bash
# 컨테이너 로그 확인
docker logs your-filebeat

# 설정 파일 검증
docker exec your-filebeat filebeat test config

# 권한 확인
docker exec your-filebeat ls -la /var/run/docker.sock
```

#### 2. Logstash 연결 실패
```bash
# Logstash 상태 확인
curl -X GET "localhost:9600/_node/stats"

# 파이프라인 상태 확인
curl -X GET "localhost:9600/_node/pipelines"

# Elasticsearch 연결 테스트
curl -u elastic:password "http://localhost:9200/_cluster/health"
```

#### 3. 로그가 Kibana에 나타나지 않는 경우
```bash
# Elasticsearch 인덱스 확인
curl -u elastic:password "http://localhost:9200/_cat/indices?v"

# 문서 수 확인
curl -u elastic:password "http://localhost:9200/your-service-logs-*/_count"

# 샘플 문서 확인
curl -u elastic:password "http://localhost:9200/your-service-logs-*/_search?size=1"
```

### 로그 레벨 설정

#### application.yml에서 로그 레벨 조정
```yaml
logging:
  level:
    your.package: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
```

#### 특정 패키지 로그 비활성화
```yaml
logging:
  level:
    org.apache.kafka: WARN
    org.elasticsearch: WARN
```

## 📊 성능 최적화

### Filebeat 최적화
```yaml
# filebeat.yml 성능 설정
filebeat.inputs:
- type: container
  # 배치 처리 설정
  harvester_buffer_size: 16384
  max_bytes: 10485760

# 출력 최적화
output.logstash:
  hosts: ["localhost:5044"]
  worker: 1
  bulk_max_size: 2048
  template.settings:
    index.number_of_shards: 1
    index.number_of_replicas: 0
```

### Logstash 최적화
```ruby
# pipeline.yml 설정
- pipeline.id: main
  pipeline.workers: 4
  pipeline.batch.size: 125
  pipeline.batch.delay: 50
```

## 🔐 보안 설정

### SSL/TLS 설정
```yaml
# Filebeat에서 SSL 설정
output.logstash:
  hosts: ["logstash:5044"]
  ssl.certificate_authorities: ["/etc/certs/ca.crt"]
  ssl.certificate: "/etc/certs/client.crt"
  ssl.key: "/etc/certs/client.key"
```

### 네트워크 보안
```yaml
# 방화벽 포트 설정
ports:
  - "5044:5044"  # Logstash Beats input
  - "9200:9200"  # Elasticsearch HTTP
  - "5601:5601"  # Kibana
```

## 📚 추가 리소스

- [Filebeat 공식 문서](https://www.elastic.co/guide/en/beats/filebeat/current/index.html)
- [Logstash 공식 문서](https://www.elastic.co/guide/en/logstash/current/index.html)
- [Elasticsearch 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Kibana 공식 문서](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)

## 📝 체크리스트

### 새로운 마이크로서비스 추가 시
- [ ] Spring Boot 애플리케이션에 JSON 로깅 설정 추가
- [ ] Logstash 파이프라인 설정 파일 생성
- [ ] Filebeat 설정에서 서비스 이름 필터 업데이트
- [ ] Kibana에서 새로운 인덱스 패턴 생성
- [ ] 로그 수집 및 파이프라인 테스트
- [ ] 대시보드 및 알림 설정

---

💡 **참고**: 이 가이드는 검증된 설정을 기반으로 작성되었습니다. 각 환경에 맞게 설정을 조정하여 사용하세요.