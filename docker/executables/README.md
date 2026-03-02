
# lcIngestionApi Service Scripts - Usage Guide

This guide explains how to run the different scripts to build, run, and manage the lcIngestionApi service and related components. Both Windows (`.bat`) and Linux/macOS (`.sh`) versions are available.

---

## 1. Compile lcIngestionApi Service

- **Windows:**  
  Run `compile_lcIngestionApi_service.bat` to compile the lcIngestionApi service.

  ```bat
  compile_lcIngestionApi_service.bat
  ```

- **Linux/macOS:**  
  Run `compile_lcIngestionApi_service.sh` to compile the lcIngestionApi service.

  ```bash
  ./compile_lcIngestionApi_service.sh
  ```

---

## 2. Run lcIngestionApi Service Docker Image

- **Windows:**  
  Run `run_lcIngestionApi_service_image.bat` to build and start the lcIngestionApi service Docker image.

  ```bat
  run_lcIngestionApi_service_image.bat
  ```

- **Linux/macOS:**  
  Run `run_lcIngestionApi_service_image.sh` to build and start the lcIngestionApi service Docker image.

  ```bash
  ./run_lcIngestionApi_service_image.sh
  ```

---

## 3. Run Full lcIngestionApi Docker Setup

- **Windows:**  
  Run `run_lcIngestionApi_full.bat` to start the full lcIngestionApi Docker environment, including all required services and application.

  ```bat
  run_lcIngestionApi_full.bat
  ```

- **Linux/macOS:**  
  Run `run_lcIngestionApi_full.sh` to start the full lcIngestionApi Docker environment, including all required services and application.

  ```bash
  ./run_lcIngestionApi_full.sh
  ```

---

## 4. Run Full lcIngestionApi with compiling, images and run Services (if applicable)

- **Windows:**  
  - `run_lcIngestionApi_Docker.bat` — Compile, image generation and runs liquidcars ingestion service db

- **Linux/macOS:**  
  - `run_lcIngestionApi_Docker.sh` — Compile, image generation and runs liquidcars ingestion service db

---

## 5. Run Redis

- **Windows:**  
  Run `run_redis.bat` to start the Redis server.

  ```bat
  run_redis.bat
  ```

- **Linux/macOS:**  
  Run `run_redis.sh` to start the Redis server.

  ```bash
  ./run_redis.sh
  ```

---

## Notes

- If you like to run lcIngestionApi independent in your code editor and you wants to connect to keycloak, you should have to have an entry in your hosts file of your OS like this 127.0.0.1 keycloak. Also is advisable add mongodb to host.

- Make sure all `.sh` scripts have execution permission on Linux/macOS:

  ```bash
  chmod +x *.sh
  ```

- The scripts should be run from the directory where they are located or provide the relative path accordingly.

- Environment variables should be properly set or configured in your environment before running the scripts if needed.

---

If you encounter any issues or need further assistance, please contact the development team.

### If using podman

- Podman needs to know where to fetch the images from. Ideally we should always use mcr.microsoft.com (since we are deploying on Azure), but for migrating I have kept the docker registry.

  ```
  mkdir -p $HOME/.config/containers/
  echo -e "[registries.search]\nregistries = ['docker.io']" | tee $HOME/.config/containers/registries.conf
  ```

