using System;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using Confluent.Kafka;
using System.Text.Json;
using CoinbasePro;
using CoinbasePro.WebSocket.Types;
using System.Collections.Generic;
using System.Collections.Concurrent;

namespace Producer
{
    class Program
    {
        static ConcurrentQueue<Trade> Queue;
        static async Task Main(string[] args)
        {
            Queue = new ConcurrentQueue<Trade>();
            var appConfig = new AppConfig("appsettings.json");
            string server = appConfig.Get<string>("server");
            var products = appConfig.Get<List<string>>("products");

            var coinbaseClient = new CoinbaseProClient();

            var socket = coinbaseClient.WebSocket;
            socket.OnMatchReceived += (sender, args) => Queue.Enqueue(new Trade(args.LastOrder.TradeId, args.LastOrder.Time.Date, args.LastOrder.Price));

            socket.Start(products, new List<ChannelType>() { ChannelType.Matches });
            var tokenSource = new CancellationTokenSource();

            await Task.Run(() => Run(new ProducerConfig
            {
                BootstrapServers = server,
                ClientId = Dns.GetHostName(),
            }, tokenSource.Token));
        }

        private static async Task Run(ProducerConfig config, CancellationToken token)
        {
            using (var producer = new ProducerBuilder<Null, string>(config).Build())
            {
                while (!token.IsCancellationRequested)
                {
                    if (!Queue.TryDequeue(out var trade))
                        continue;

                    var json = JsonSerializer.Serialize(trade, new JsonSerializerOptions() { PropertyNameCaseInsensitive = true });
                    var result = await producer.ProduceAsync("myTestTopic", new Message<Null, string>() { Value = json }, CancellationToken.None);
                }
            }
        }
    }
}
