FROM rust:latest as rust

RUN rustup target add x86_64-unknown-linux-musl \
    && cargo install --target=x86_64-unknown-linux-musl just

FROM openapitools/openapi-generator-cli:v7.0.0 as maven

RUN apt update && apt -y install jq git maven gettext-base libicu-dev default-jdk-headless
COPY --from=rust /usr/local/cargo/bin/just /usr/bin/just

RUN mkdir -p /usr/src/
WORKDIR /usr/src/

# Make ssh dir
# Create known_hosts
# Add github key
RUN mkdir /root/.ssh/ \
    && touch /root/.ssh/known_hosts \
    && ssh-keyscan github.com >> /root/.ssh/known_hosts

RUN --mount=type=ssh \
    git clone git@github.com:finbourne/lusid-sdk-doc-templates.git /tmp/docs \
    && git clone git@github.com:finbourne/lusid-sdk-workflow-template.git /tmp/workflows

COPY generate/ /usr/src/generate
COPY ./justfile /usr/src/