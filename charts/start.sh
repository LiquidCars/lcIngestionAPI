kubectl create secret docker-registry acr-secret --docker-server='lawyerty.azurecr.io' --docker-username=lawyerly --docker-password='k6/xE/Ezn7pApV/FLD25X/YiE5hIukpjtbIgK29IYH+ACRBUUlLI' --docker-email='notused@example.com'

helm install lawyerlyfe ./chart/
