
----- config service post script starts from here ------

wrk.method = "POST"
wrk.body   = [[// This describes a container containing the HCD and is used to start the actors.
container {
  name = "tromboneHCD"
  connectionType: [akka]
  components {
    lgsTromboneHCD {
      type = HCD
      class = "csw.examples.vslice.hcd.TromboneHCD"
      prefix = nfiraos.ncc.tromboneHCD
      connectionType: [akka]
      rate = 1 second
    }
  }
}

// Additional, application specific configuration for the HCD
csw.examples.trombone.hcd {
  axis-config {
    axisName = "tromboneAxis"
    lowLimit = 100
    lowUser = 200
    highUser = 1200
    highLimit = 1300
    home = 300
    startPosition = 350
    stepDelayMS = 100  // This value can be shorter, but 150 shows well in tests
  }
}

// This describes a container containing the HCD and is used to start the actors.
container {
  name = "tromboneHCD"
  connectionType: [akka]
  components {
    lgsTromboneHCD {
      type = HCD
      class = "csw.examples.vslice.hcd.TromboneHCD"
      prefix = nfiraos.ncc.tromboneHCD
      connectionType: [akka]
      rate = 1 second
    }
  }
}]]

-- example dynamic request script which demonstrates changing
-- the request path and a header for each request
-------------------------------------------------------------
-- NOTE: each wrk thread has an independent Lua scripting
-- context and thus there will be one counter per thread

request = function()
   path = "/config/trombone/test/normal/hcd_conf_" .. math.random(0, 500000000)
   return wrk.format(nil, path)
end


----- debug script start from here -----
-- Helper Functions:

-- Resource: http://lua-users.org/wiki/TypeOf
function typeof(var)
    local _type = type(var);
    if(_type ~= "table" and _type ~= "userdata") then
        return _type;
    end
    local _meta = getmetatable(var);
    if(_meta ~= nil and _meta._NAME ~= nil) then
        return _meta._NAME;
    else
        return _type;
    end
end

-- Resource: https://gist.github.com/lunixbochs/5b0bb27861a396ab7a86
local function stringF(o)
    return '"' .. tostring(o) .. '"'
end

local function recurse(o, indent)
    if indent == nil then indent = '' end
    local indent2 = indent .. '  '
    if type(o) == 'table' then
        local s = indent .. '{' .. '\n'
        local first = true
        for k,v in pairs(o) do
            if first == false then s = s .. ', \n' end
            if type(k) ~= 'number' then k = stringF(k) end
            s = s .. indent2 .. '[' .. k .. '] = ' .. recurse(v, indent2)
            first = false
        end
        return s .. '\n' .. indent .. '}'
    else
        return stringF(o)
    end
end

local function var_dump(...)
    local args = {...}
    if #args > 1 then
        var_dump(args)
    else
        print(recurse(args[1]))
    end
end

-- @end: Helper Functions

max_requests = 0
counter = 1

function setup(thread)
   thread:set("id", counter)

   counter = counter + 1
end

init = function(args)
  io.write("[init]\n")

  -- Check if arguments are set
  if not (next(args) == nil) then
    io.write("[init] Arguments\n")

    -- Loop through passed arguments
    for index, value in ipairs(args) do
      io.write("[init]  - " .. args[index] .. "\n")
    end
  end
end

response = function (status, headers, body)
  io.write("------------------------------\n")
  io.write("Response ".. counter .." with status: ".. status .." on thread ".. id .."\n")
  io.write("------------------------------\n")

  io.write("[response] Headers:\n")

  -- Loop through passed arguments
  for key, value in pairs(headers) do
    io.write("[response]  - " .. key  .. ": " .. value .. "\n")
  end

  io.write("[response] Body:\n")
  io.write(body .. "\n")

  -- Stop after max_requests if max_requests is a positive number
  if (max_requests > 0) and (counter > max_requests) then
    wrk.thread:stop()
  end

  counter = counter + 1
end

done = function (summary, latency, requests)
  io.write("------------------------------\n")
  io.write("Requests\n")
  io.write("------------------------------\n")

  io.write(typeof(requests))

  var_dump(summary)
  var_dump(requests)

   io.write("------------------------------\n")
   io.write("----- Latency Percentile -----\n")
   io.write("------------------------------\n")
   for _, p in pairs({ 50, 90, 99, 99.999 }) do
      n = latency:percentile(p)
      io.write(string.format("%g%%,%d\n", p, n))
   end

end
