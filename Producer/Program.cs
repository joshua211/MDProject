using System;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using Confluent.Kafka;
using System.Text.Json;

namespace Producer
{
    class Program
    {
        static async Task Main(string[] args)
        {
            var config = new ProducerConfig
            {
                BootstrapServers = "172.22.160.151:9092",
                ClientId = Dns.GetHostName(),
            };

            using (var producer = new ProducerBuilder<Null, string>(config).Build())
            {
                var test = new TestClass()
                {
                    Id = 1,
                    Name = "Test haha"
                };
                var json = JsonSerializer.Serialize(test, new JsonSerializerOptions() { PropertyNameCaseInsensitive = true });
                var result = await producer.ProduceAsync("myTestTopic", new Message<Null, string>() { Value = json }, CancellationToken.None);
                System.Console.WriteLine(result.Status);
            }
        }
    }
}
