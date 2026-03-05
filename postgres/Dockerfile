FROM postgis/postgis:18-3.6

# Install dependencies for pgvector
RUN apt-get update && apt-get install -y --no-install-recommends postgresql-server-dev-18 build-essential git ca-certificates \
    && git clone --branch v0.8.1 https://github.com/pgvector/pgvector.git /pgvector \
    && cd /pgvector && make && make install \
    && cd / \
    && rm -rf /pgvector \
    && apt-get remove -y build-essential git postgresql-server-dev-18 \
    && apt-get autoremove -y \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Configure PostgreSQL to load pgvector
RUN echo "shared_preload_libraries = 'vector'" >> /etc/postgresql/postgresql.conf.sample
