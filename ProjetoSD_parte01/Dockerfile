FROM python:3.11-slim

WORKDIR /app

RUN pip install pyzmq msgpack

COPY . .

CMD ["python", "main.py"]
