FROM mcr.microsoft.com/playwright:v1.43.0-jammy

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .

# Install fonts (IMPORTANT for Hindi + emojis)
RUN apt-get update && apt-get install -y \
    fonts-noto \
    fonts-noto-cjk \
    fonts-noto-color-emoji \
    fonts-noto-unhinted \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 3001

CMD ["npm", "start"]
