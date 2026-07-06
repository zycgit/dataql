var dataSet = @@insert(itemCode = "abc", status = true, extra, pageSize = 10, page = 1) <% INSERT INTO users (name, age) VALUES (:itemCode, :status) %>

return dataSet("X", false, true, 20, 2) => [
    { "id", "name", "code" }
]
