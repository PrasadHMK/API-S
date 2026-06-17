# ---------- Stage 1: Build ----------
    FROM registry.hallmarklabs.com/enterprise-integrations/images/maven:21-graal-v1.0.2 as builder

    WORKDIR /build
    COPY . .
    ARG CODEARTIFACT_AUTH_TOKEN    
    RUN echo "***** Inside Docker: CODEARTIFACT_AUTH_TOKEN is: $CODEARTIFACT_AUTH_TOKEN"
    RUN mvn clean -Pnative native:compile -s settings.xml -DskipTests
    
    # ---------- Stage 2: Runtime ----------
    FROM registry.hallmarklabs.com/enterprise-integrations/images/maven:21-graal-v1.0.2
    
    ENV PATH=$PATH:/app
    
    WORKDIR /app
    COPY --from=builder /build/target/hmk-purchases-papi hmk-purchases-papi


    CMD ["hmk-purchases-papi"]