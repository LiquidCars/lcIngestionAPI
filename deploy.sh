#! /bin/bash
#
# Create the jar file, and upload it to the ACR.
# Deploy the application with helm
#
DEBUG=false
PARAM=""
PROJECT="liquidcars"
ENV="dev"
MODULE="ingestion"

usage() {
    echo "Help:"
    echo "-e environment dev,test or prod"
    echo "-p project_name. lawyerty, liquidcars, etc."
    echo "-d enable debug."
    exit -1;
 }

[ $# -eq 0 ] && usage

while getopts ":he:p:d" opt; do
 case $opt in
  e)
      echo "option environment: $OPTARG"
      export ENV=${OPTARG}

      if [[ "${ENV}" != "dev" && "${ENV}" != "test" && "${ENV}" != "prod" ]]; then
         echo "Invalid environment variable." >&2
         usage
      fi
      ;;
  p)
      echo "option project: $OPTARG"
      PROJECT=${OPTARG}
      ;;
  d)
      echo 'Enable debug mode/dry run'
      DEBUG=true
      ;;
  h | *)
      usage
      exit -1
      ;;
 esac
done

if [ -z "${ENV}" ] || [ -z "${PROJECT}" ]; then
    usage
fi

if [ "$DEBUG" = true ]; then
  PARAM="--debug --dry-run"
fi

export NS="${PROJECT}-${ENV}"
export KUBE="aks-lc-${ENV}-eu"

kubectl config use-context ${KUBE}

# Create the namespace if it does not exist.
kubectl get namespace ${NS} || kubectl create namespace ${NS}

# Change to the correct namespace
kubectl config set-context --current --namespace=${NS}

HELM_VERSION=$(helm version --template='{{.Version}}' | sed 's/^v//')
if [ "${HELM_VERSION}" != "3.19.5" ]; then
   print "** WARNING **: expecting helm >3.19.x. Check using: helm version"
fi

# Needed for the jib deploy. Later we will change this to an env variable.
export ACR_CREDENTIALS_USR="liquidcars"
export ACR_CREDENTIALS_PWD="wpXdKUJPs5HCbYe0NaZPUxrKSgIT0ZGlEIru4FzSUB+ACRAQBSFR"

#export ACR_CREDENTIALS_USR="$(kubectl get secret acr-secret -o jsonpath="{.data.\\.dockerconfigjson}" | base64 --decode | jq '.auths."lawyerty.azurecr.io".username')"
#export ACR_CREDENTIALS_PWD="$(kubectl get secret acr-secret -o jsonpath="{.data.\\.dockerconfigjson}" | base64 --decode | jq '.auths."lawyerty.azurecr.io".password')"

# Skip deploy in DEBUG mode
if [ "$DEBUG" = false ]; then
  ./gradlew jib
fi

# For the dependencies in Chart.yaml
helm repo add strimzi https://strimzi.io/charts/
helm repo add mongodb https://mongodb.github.io/helm-charts
helm repo update
helm dependency update charts/
# Manually install the CRDs - makes the chart simpler.
kubectl apply -f https://raw.githubusercontent.com/mongodb/mongodb-kubernetes-operator/master/config/crd/bases/mongodbcommunity.mongodb.com_mongodbcommunity.yaml
# Make sure helm owns these crds...
kubectl label crd mongodbcommunity.mongodbcommunity.mongodb.com \
  app.kubernetes.io/managed-by=Helm
kubectl annotate crd mongodbcommunity.mongodbcommunity.mongodb.com \
  meta.helm.sh/release-name=ingestion \
  meta.helm.sh/release-namespace=liquidcars-${ENV}


# Gradle will have increased the version, so we use this.
APPLICATION_VERSION=`cat version.txt`
sed -i -E "s/appVersion: .*/appVersion: \"${APPLICATION_VERSION}\"/" charts/Chart.yaml
version=${APPLICATION_VERSION}

# And if needed, check the version uploaded
#az acr repository list --name liquidcars --output table
#az acr repository show-tags --name liquidcars --repository liquidcars/dev/${MODULE}

if [ "$DEBUG" = true ]; then
  echo "Check charts before running...."
  helm lint charts/
  helm template \
     --values charts/values.yaml \
     --values charts/values-${ENV}.yaml \
     --set image.tag=${version} \
     --set installCRs=false
     ${MODULE} charts/
fi

# use a flag to skip CR templates on first run - so that kafka and mongo are ready.
# install operators (and their CRDs)
echo helm upgrade ${PARAM} --install --namespace=${NS} \
     ${MODULE} charts/ \
     --values charts/values.yaml \
     --values charts/values-${ENV}.yaml \
     --set image.tag=${version} \
     --set installCRs=false \
     --wait \
    --timeout 2m

# Deploy the new image to kubernetes in the correct namespace
helm upgrade ${PARAM} --install --namespace=${NS} \
     ${MODULE} charts/ \
     --values charts/values.yaml \
     --values charts/values-${ENV}.yaml \
     --set image.tag=${version} \
     --wait \
    --timeout 2m


echo "Press a key to continue..."

read -n 1 -s
