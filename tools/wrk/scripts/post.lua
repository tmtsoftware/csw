
----- config service post script starts from here ------

function readAll(file)
    local f = io.open(file, "rb")
    local content = f:read("*all")
    f:close()
    return content
end

wrk.method = "POST"
wrk.body   = readAll('conf/tromboneHCD.conf')

-- example dynamic request script which demonstrates changing
-- the request path and a header for each request
-------------------------------------------------------------
-- NOTE: each wrk thread has an independent Lua scripting
-- context and thus there will be one counter per thread

counter = 0

request = function()
    path = "/config/app_" .. counter .. "?annex=true"
    counter = counter + 1
    return wrk.format(nil, path)
end

dofile "debug.lua"
