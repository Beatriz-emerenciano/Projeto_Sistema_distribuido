FROM python:3.11-slim

WORKDIR /app

COPY . .

RUN pip install pyzmq msgpack

CMD ["python", "coordenador.py"]