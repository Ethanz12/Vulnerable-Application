FROM mcr.microsoft.com/playwright/python:v1.49.0-jammy
WORKDIR /bot
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY review_bot.py .
EXPOSE 5000
CMD ["python", "review_bot.py"]
