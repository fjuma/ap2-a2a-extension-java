# ap2-a2a-java

## Overview

This is a Java implementation based on the Python repository [google-agentic-commerce/AP2](https://github.com/google-agentic-commerce/AP2) up to commit [60b100c2ff974d62d6da15f6abb0f26140679a91](https://github.com/google-agentic-commerce/AP2/commit/60b100c2ff974d62d6da15f6abb0f26140679a91).

**Note: This implementation is currently untested.**

## Repository Structure

- **spec/** - Contains the code for the A2A extension for AP2
- **samples/** - Contains the Java equivalent of the Python sample from [google-agentic-commerce/AP2/samples/python](https://github.com/google-agentic-commerce/AP2/tree/main/samples/python)

## Running the Sample Agents

The samples directory contains four Quarkus-based agents that demonstrate the A2A extension:

### Prerequisites
- Java 17 or later
- Maven 3.8+
- Google Gemini API key

### Build
From the project root:
```bash
mvn clean install
```

### Running Individual Agents

Each agent runs on a different port in development mode:

**1. Merchant Agent** (port 8001)
```bash
cd samples/merchant
mvn quarkus:dev
```

**2. Credentials Provider** (port 8002)
```bash
cd samples/credentials-provider
mvn quarkus:dev
```

**3. Merchant Payment Processor** (port 8003)
```bash
cd samples/merchant-payment-processor
mvn quarkus:dev
```

**4. Shopping Agent** (port 8004)
```bash
cd samples/shopping-agent
mvn quarkus:dev
```

### Running All Agents

To run the complete sample, start all four agents in separate terminal windows using the commands above.