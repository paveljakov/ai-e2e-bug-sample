# How to run

AI agent is downloaded from maven repo and copied to `build/`

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
cd ai-examle-app
gradlew bootRun
```

Run curls
```
curl -v -d "test" http://localhost:9090/example
curl -v http://localhost:9090/example
```

Check AI e2e transaction log, there should be two separate transactions for gateway and service, not a single one how it should be.
