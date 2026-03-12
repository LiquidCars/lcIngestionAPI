db.getSiblingDB('admin').createUser({
    user: 'liquidcars_service',
    pwd: 'mongo',
    roles: [{ role: 'root', db: 'admin' }]
});
