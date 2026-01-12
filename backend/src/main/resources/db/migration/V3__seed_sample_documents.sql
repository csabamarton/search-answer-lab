-- Insert sample technical documents for testing search functionality

INSERT INTO documents (title, content, source) VALUES
(
    'PostgreSQL Full-Text Search Guide',
    'PostgreSQL provides powerful full-text search capabilities using tsvector and tsquery data types. The to_tsvector function converts text to a normalized format, while to_tsquery creates searchable patterns. GIN indexes significantly improve search performance on large datasets. You can rank results using ts_rank and ts_rank_cd functions for relevance scoring.',
    'postgresql_docs'
),
(
    'Docker Compose Best Practices',
    'Docker Compose simplifies multi-container application deployment. Use version 3.8 for modern features. Always define healthchecks for databases to ensure services start in the correct order. Use named volumes for data persistence and .env files for configuration. Network isolation improves security between services.',
    'docker_guide'
),
(
    'Spring Boot Application Monitoring',
    'Spring Boot Actuator provides production-ready features for monitoring applications. The /health endpoint checks application status, while /metrics exposes performance data. Configure management.endpoints.web.exposure.include to control which endpoints are available. Use Micrometer for custom metrics and integrate with Prometheus for visualization.',
    'spring_docs'
),
(
    'React Performance Optimization',
    'Optimize React applications using useMemo and useCallback hooks to prevent unnecessary re-renders. React.memo creates memoized components that only re-render when props change. Code splitting with lazy loading reduces initial bundle size. The React DevTools Profiler helps identify performance bottlenecks in component trees.',
    'react_guide'
),
(
    'Database Indexing Strategies',
    'Database indexes improve query performance but require careful planning. B-tree indexes work well for equality and range queries. GIN indexes excel at full-text search and array operations. Analyze query patterns before creating indexes, as they add overhead to write operations. Use EXPLAIN ANALYZE to measure index effectiveness.',
    'database_performance'
),
(
    'RESTful API Design Principles',
    'RESTful APIs should use HTTP methods correctly: GET for retrieval, POST for creation, PUT for updates, and DELETE for removal. Use proper status codes: 200 for success, 201 for created resources, 404 for not found, and 500 for server errors. Version your APIs using URL prefixes like /api/v1 to maintain backward compatibility.',
    'api_design'
),
(
    'Microservices Communication Patterns',
    'Microservices communicate through synchronous REST APIs or asynchronous message queues. Service discovery helps services locate each other dynamically. Circuit breakers prevent cascading failures when services are down. Distributed tracing with tools like Zipkin helps debug cross-service requests. API gateways provide a single entry point for clients.',
    'microservices_architecture'
),
(
    'CI/CD Pipeline Configuration',
    'Continuous Integration and Continuous Deployment pipelines automate testing and deployment. GitHub Actions, GitLab CI, and Jenkins are popular tools. Pipelines typically include stages for build, test, security scanning, and deployment. Use environment-specific configurations and secrets management. Implement rollback strategies for failed deployments.',
    'devops_guide'
),
(
    'TypeScript Type Safety Benefits',
    'TypeScript adds static type checking to JavaScript, catching errors at compile time. Interfaces and types define contract shapes for objects and functions. Generics enable reusable type-safe components. The strict mode flag enables all type-checking options for maximum safety. IDE integration provides excellent autocomplete and refactoring support.',
    'typescript_handbook'
),
(
    'Kubernetes Pod Scheduling',
    'Kubernetes schedules pods across cluster nodes based on resource requirements and constraints. Node selectors and affinity rules control pod placement. Taints and tolerations prevent pods from scheduling on inappropriate nodes. Resource requests and limits ensure fair allocation. The scheduler considers CPU, memory, and custom metrics when making decisions.',
    'kubernetes_operations'
);

-- Verify the inserts
SELECT COUNT(*) as document_count FROM documents;
