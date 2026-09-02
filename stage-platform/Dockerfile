# ============================================================
#  stage-platform (Angular 21) - build Node puis nginx
#
#  Le bundle est construit ici : aucun node_modules ni dist/
#  n'est copie depuis la machine (voir .dockerignore).
# ============================================================

# ---------- etape 1 : build ----------
# node:24 et non node:22 : package-lock.json a ete genere par npm 11
# (voir "packageManager" dans package.json). node:22-alpine embarque
# npm 10, qui resout un arbre different et fait echouer "npm ci" sur
# des dependances natives optionnelles absentes du verrou.
FROM node:24-alpine AS build
WORKDIR /app

# Les manifestes d'abord : cette couche est mise en cache tant que
# les dependances ne changent pas.
COPY package.json package-lock.json ./

# "npm install" et non "npm ci" : @tailwindcss/oxide-wasm32-wasi tire
# @napi-rs/wasm-runtime, qui exige @emnapi/core et @emnapi/runtime.
# npm sous Windows ne juge pas ces paquets installables et ne les
# inscrit jamais dans package-lock.json, meme avec --os=linux ; sous
# Linux "npm ci" les reclame et echoue (EUSAGE, "Missing: @emnapi/core").
# "npm install" resout l'arbre dans le conteneur et complete ce trou.
RUN npm install --no-audit --no-fund

COPY . .
RUN npx ng build --configuration production

# ---------- etape 2 : runtime ----------
FROM nginx:alpine

# @angular/build:application ecrit dans dist/<projet>/browser
COPY --from=build /app/dist/stage-platform/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
