
using System;
using System.Collections.Generic;
using System.IO;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace Producer
{
    class AppConfig
    {
        private readonly Dictionary<string, object> settings;

        public AppConfig(string fileName)
        {
            if (File.Exists(fileName))
            {
                var json = File.ReadAllText(fileName);
                settings = JsonConvert.DeserializeObject<Dictionary<string, object>>(json);
            }
        }

        public T Get<T>(string name) where T : class
        {
            try
            {
                var env = Environment.GetEnvironmentVariable(name);
                if (env is not null and T val)
                    return val;
                if (settings.TryGetValue(name, out var value))
                    return value is JArray ? ((JArray)value).ToObject<T>() : value as T;
                else
                    return default(T);
            }
            catch { }

            return default(T);
        }
    }
}