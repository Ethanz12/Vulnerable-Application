FROM mcr.microsoft.com/playwright/python:v1.49.0-jammy
WORKDIR /bot
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY review_bot.py .
RUN mkdir -p /bot/screenshots \
    && useradd -r -s /bin/false botuser \
    && chown -R botuser:botuser /bot
EXPOSE 5000
USER botuser
CMD ["python", "review_bot.py"]
