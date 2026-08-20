# ============================================================
#  ml-service - prediction du risque de stage
#
#  Le modele est ENTRAINE PENDANT LE BUILD : l'image contient
#  toujours un modele coherent avec le code, et aucun binaire
#  .pkl n'a besoin d'etre versionne dans Git.
# ============================================================

FROM python:3.12-slim

WORKDIR /app

# Dependances d'abord : couche mise en cache tant que
# requirements.txt ne change pas.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# Entrainement a la construction de l'image.
RUN python -m app.train

# Ne jamais tourner en root dans un conteneur.
RUN useradd --create-home --shell /bin/false app \
 && chown -R app:app /app
USER app

EXPOSE 8087

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8087/health').status==200 else 1)"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8087"]
