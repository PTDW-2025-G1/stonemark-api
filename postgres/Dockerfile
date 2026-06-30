FROM postgis/postgis:18-3.6

RUN apt-get update && apt-get install -y \
    git \
    build-essential \
    postgresql-server-dev-18

RUN git clone --branch v0.8.3 https://github.com/pgvector/pgvector.git \
    && cd pgvector \
    && make \
    && make install \
    && cd .. \
    && rm -rf pgvector \
