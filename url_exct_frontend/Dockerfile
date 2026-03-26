FROM node:14-alpine as nodework

WORKDIR /myapp

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build



FROM nginx:1.23-alpine

WORKDIR /usr/share/nginx/html

RUN rm -rf ./*

COPY --from=nodework /myapp/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf

ENTRYPOINT [ "nginx","-g","daemon off;" ]
