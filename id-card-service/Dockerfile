FROM python:3.12.4-slim

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1
ARG PYTHONPATH=""
ENV PYTHONPATH=/app/src:${PYTHONPATH}

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    build-essential \
    libgl1-mesa-glx \
    libglib2.0-0 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt /app/requirements.txt

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r /app/requirements.txt

COPY . /app

RUN pip install -e .

EXPOSE 5001

CMD ["python", "server.py"]