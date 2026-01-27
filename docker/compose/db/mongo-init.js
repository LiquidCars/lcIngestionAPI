db.getSiblingDB('admin').createUser({
    user: 'ingestion_service',
    pwd: 'wku1kfm0xex_GJR9zgp',
    roles: [{ role: 'root', db: 'admin' }]
});
