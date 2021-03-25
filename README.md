# How to run

Update `gradle.properties` with AI connection string
```
aiConnectionString=...
```

Run sample service app (runs on port 9091)
```
cd ai-examle-app
gradlew bootRun
```

Run sample gateway app (runs on port 9090)
```
cd ai-sample-gateway
gradlew bootRun
```

Run curls
```
curl -v -d "test" http://localhost:9090/example
curl -v http://localhost:9090/example
```

Check AI e2e transaction log, there will be two separate transactions for gateway and service, not a single one how it should be. Everyting is working with AI agent version 3.0.2 and breaks with 3.0.3-BETA.*
