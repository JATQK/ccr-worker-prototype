# GitRDF Worker

Spring Boot microservice for transforming Git repositories and GitHub data into RDF format – enabling semantic analysis and advanced querying of version control data.

GitRDF Worker is a specialized service that processes Git repositories and GitHub metadata, converting them into Resource Description Framework (RDF) triples for semantic web applications and advanced analytics.

## Project Setup and Deployment

For comprehensive setup instructions and project management, please refer to the main deployment repository:

**[Project Deployment Compose](https://github.com/git2RDFLab/project-deployment-compose)**

The deployment compose is the main interactive service to monitor and start/restart/build the project. It provides a centralized way to manage all project components and their dependencies.

## Getting Started

### Prerequisites

Before diving in, ensure you have:

- **Java 21** or higher installed
- **Docker and Docker Compose** installed and running
- **PostgreSQL 15.5** (automatically provided via Docker Compose)
- **GitHub App** configured for repository access (optional, for GitHub integration)
- **Maven** (or use the included Maven wrapper)

### Quick Start

```bash
# Start the PostgreSQL database
docker-compose up -d

# Build the application
./mvnw clean package

# Run the application
java -jar target/worker-1.0.0-SNAPSHOT.jar
```

Or use Docker:

```bash
# Build the Docker image
docker build -t gitrdf-worker .

# Run with Docker Compose
docker-compose up
```

The application will be available at `http://localhost:28099`

## Configuration

### Database Configuration

The application uses PostgreSQL as its primary database. Configure connection details via environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gitrdfdb
export SPRING_DATASOURCE_PASSWORD=your_password
```

### GitHub Integration

For GitHub repository processing, configure your GitHub App credentials:

#### Single Account Configuration

```bash
export GITHUB_LOGIN_KEY=your_private_key
export GITHUB_LOGIN_APP_ID=your_app_id
export GITHUB_LOGIN_APP_INSTALLATION_ID=your_installation_id
```

#### Multi-Account Configuration

Support for multiple GitHub accounts using numbered suffixes:

```bash
# Account 1
export GITHUB_LOGIN_KEY_1=account1_private_key
export GITHUB_LOGIN_APP_ID_1=account1_app_id
export GITHUB_LOGIN_APP_INSTALLATION_ID_1=account1_installation_id

# Account 2
export GITHUB_LOGIN_KEY_2=account2_private_key
export GITHUB_LOGIN_APP_ID_2=account2_app_id
export GITHUB_LOGIN_APP_INSTALLATION_ID_2=account2_installation_id
```

### Worker Configuration

Customize processing behavior:

```bash
export WORKER_TASK_RDFGITREPO_ENABLED=true
export WORKER_TASK_RDFGITHUBREPO_ENABLED=true
```

## Features

### Core Functionality

- **Git Repository Processing**: Transform Git commit history, branches, and metadata into RDF
- **GitHub Integration**: Process GitHub issues, pull requests, and repository metadata
- **Multi-Account Support**: Handle multiple GitHub accounts simultaneously
- **Rate Limit Management**: Intelligent handling of GitHub API rate limits
- **Batch Processing**: Configurable commit processing in batches (default: 100 commits per iteration)
- **Comment Export**: Optional export of issue and pull request comments

### Database Integration

- **PostgreSQL Support**: Optimized for PostgreSQL 15.5+
- **Flyway Migrations**: Automated database schema management
- **Spring Integration Tables**: Built-in message processing and coordination tables
- **Hibernate ORM**: JPA-based data persistence

### Monitoring & Logging

- **Configurable Logging**: Detailed logging for debugging and monitoring
- **SQL Query Logging**: Optional Hibernate SQL logging for development
- **Rate Limit Monitoring**: Track GitHub API usage and limits

## Architecture

### Technology Stack

- **Spring Boot 3.2.2**: Core application framework
- **Java 21**: Runtime environment (Amazon Corretto)
- **PostgreSQL 15.5**: Primary database
- **Flyway**: Database migration management
- **Hibernate/JPA**: Object-relational mapping
- **Spring Integration**: Message processing framework

### Processing Flow

1. **Repository Analysis**: Parse Git repository structure and history
2. **Data Transformation**: Convert Git objects to RDF triples
3. **GitHub Integration**: Fetch and process GitHub metadata
4. **Rate Limit Management**: Respect GitHub API limits
5. **Data Persistence**: Store processed data in PostgreSQL
6. **RDF Export**: Generate semantic web-compatible output

## Development

### Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd gitrdf-worker

# Build with Maven
./mvnw clean package

# Run tests
./mvnw test
```

### Docker Development

```bash
# Build Docker image
docker build -t gitrdf-worker .

# Run with development profile
docker run -p 28099:28099 \
  -e SPRING_PROFILES_ACTIVE=dev \
  gitrdf-worker
```

### Database Setup

The application automatically creates necessary tables using Flyway migrations. For manual setup:

```bash
# Connect to PostgreSQL
psql -h localhost -U root -d gitrdfdb

# Run schema creation (if needed)
\i src/main/resources/schema-postgresql.sql
```

## Configuration Reference

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `worker.commits-per-iteration` | `100` | Number of commits processed per batch |
| `worker.issues.export-comments` | `true` | Include issue/PR comments in export |
| `github.rate-limit.requests-left-border` | `50` | Threshold for rate limit waiting |
| `server.port` | `28099` | Application server port |

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | No | PostgreSQL connection URL |
| `SPRING_DATASOURCE_PASSWORD` | No | Database password |
| `GITHUB_LOGIN_KEY` | No | GitHub App private key |
| `GITHUB_LOGIN_APP_ID` | No | GitHub App ID |
| `WORKER_TASK_RDFGITREPO_ENABLED` | No | Enable Git repository processing |
| `WORKER_TASK_RDFGITHUBREPO_ENABLED` | No | Enable GitHub repository processing |

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| Database connection failed | Ensure PostgreSQL is running and credentials are correct |
| GitHub API rate limit exceeded | Configure multiple GitHub accounts or adjust rate limit settings |
| Out of memory during processing | Reduce `commits-per-iteration` value |
| Docker build fails | Ensure target JAR file exists (`./mvnw package` first) |

### Debugging

Enable debug logging for troubleshooting:

```yaml
logging:
  level:
    de.leipzig.htwk.gitrdf.worker: DEBUG
    org.hibernate.SQL: DEBUG
```

### Health Checks

The application exposes standard Spring Boot actuator endpoints:

- Health: `GET /actuator/health`
- Info: `GET /actuator/info`
- Metrics: `GET /actuator/metrics`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.

## Support

For questions, issues, or contributions, please refer to the project's issue tracker or contact the development team at HTWK Leipzig.

---

**GitRDF Worker** - Transforming version control data into semantic knowledge graphs.