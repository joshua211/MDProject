using System;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using Confluent.Kafka;
using System.Text.Json;
using CoinbasePro;
using CoinbasePro.WebSocket.Types;
using System.Collections.Generic;
using CoinbasePro.WebSocket.Models.Response;

namespace Producer
{
    class Program
    {
        static async Task Main(string[] args)
        {
            const string server = "172.22.160.151:9092";
            const string product = "BTC-EUR";

            var config = new ProducerConfig
            {
                BootstrapServers = server,
                ClientId = Dns.GetHostName(),
            };
            var coinbaseClient = new CoinbaseProClient();

            var socket = coinbaseClient.WebSocket;
            socket.Start(new List<string>() { product }, new List<ChannelType>() { ChannelType.Matches });
            socket.OnMatchReceived += (sender, args) => WriteToTopic(config, new { TradeId = args.LastOrder.TradeId, Price = args.LastOrder.Price, Time = args.LastOrder.Time });

            Console.ReadKey();
        }

        private static async void WriteToTopic(ProducerConfig config, object trade)
        {
            using (var producer = new ProducerBuilder<Null, string>(config).Build())
            {
                var json = JsonSerializer.Serialize(trade, new JsonSerializerOptions() { PropertyNameCaseInsensitive = true });
                var result = await producer.ProduceAsync("myTestTopic", new Message<Null, string>() { Value = json }, CancellationToken.None);
            }
        }
    }
}
