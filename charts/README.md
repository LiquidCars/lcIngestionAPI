kubectl -n lawyerty-dev create secret docker-registry acr-secret --docker-server='lawyerty.azurecr.io' --docker-username=lawyerly --docker-password='k6/xE/Ezn7pApV/FLD25X/YiE5hIukpjtbIgK29IYH+ACRBUUlLI' --docker-email='notused@example.com'

kubectl create namespace lawyerly-dev

helm -n lawyerly-dev upgrade --install lawyerlybe charts

 kpf service/lawyerty-backend -n default 8082:80
 curl -v http://localhost:8082/api/v1/globalization/languages
 
