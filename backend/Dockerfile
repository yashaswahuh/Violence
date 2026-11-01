# Violence Jam server. Build context is the backend/ folder:
#   docker build -t violence-jam backend
#   docker run -p 8000:8000 -e PORT=8000 violence-jam
#
# go.mod asks for a newer Go than the build image; GOTOOLCHAIN=auto lets Go fetch
# exactly the version go.mod names, so this file does not need editing when go.mod moves.
FROM golang:1.23-alpine AS build
ENV GOTOOLCHAIN=auto CGO_ENABLED=0
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -trimpath -ldflags="-s -w" -o /out/server .

# Static binary, no shell, no root, no secrets: the server holds no credentials.
FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /out/server /server
ENV PORT=8000
EXPOSE 8000
USER nonroot:nonroot
ENTRYPOINT ["/server"]