
----- config service get script starts from here ------

wrk.method = "GET"

-- example dynamic request script which demonstrates changing
-- the request path and a header for each request
-------------------------------------------------------------
-- NOTE: each wrk thread has an independent Lua scripting
-- context and thus there will be one counter per thread

counter = 0

request = function()
    path = "/config/app_" .. counter
    counter = counter + 1
    return wrk.format(nil, path)
end


dofile "debug.lua"
