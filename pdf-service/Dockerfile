FROM node:20-slim

WORKDIR /app

# Install system libraries required by Chrome headless at runtime.
# These are the minimal shared objects (.so) Chrome links against.
# fonts-noto-core/cjk ensure text renders correctly in generated PDFs.
# --no-install-recommends avoids pulling unnecessary extras.
# apt cache is purged to keep the layer small.
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget ca-certificates \
    libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 libdrm2 \
    libxkbcommon0 libxcomposite1 libxdamage1 libxfixes3 \
    libxrandr2 libgbm1 libpango-1.0-0 libcairo2 libasound2 \
    fonts-noto-core fonts-noto-cjk \
    && rm -rf /var/lib/apt/lists/*

# Copy dependency manifests first for Docker layer caching —
# node_modules layer is rebuilt only when package.json changes.
# postinstall script in package.json downloads chrome-headless-shell (~50MB),
# a stripped-down Chrome binary that only supports headless mode.
COPY package.json package-lock.json* ./
RUN npm install

# Create a stable symlink so server.js can reference a fixed path
# regardless of the chrome-headless-shell version number in the directory name.
RUN ln -s $(find /app -name "chrome-headless-shell" -type f | head -1) /usr/local/bin/chrome-headless-shell

# Copy application source code
COPY . .

EXPOSE 3001

CMD ["npm", "start"]
