db.getSiblingDB('admin').createUser({
    user: 'ingestion_service',
    pwd: 'mongo',
    roles: [{ role: 'root', db: 'admin' }]
});
