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
using Serilog;
using Serilog.Events;
using Serilog.Filters;

namespace Producer
{
    class Program
    {
        private static ConcurrentQueue<Trade> queue;
        private static AppConfig config;

        static async Task Main(string[] args)
        {
            Log.Logger = new LoggerConfiguration()
                        .MinimumLevel.Information()
                        .Enrich.FromLogContext()
                        .WriteTo.Console().MinimumLevel.Debug()
                        .WriteTo.File("./Log.log", LogEventLevel.Information)
                        .CreateLogger();

            queue = new ConcurrentQueue<Trade>();

            config = new AppConfig("appsettings.json");
            string server = config.Get<string>("server");
            var products = config.Get<List<string>>("products");

            Log.Information("Starting connector for server {Server}", server);

            var coinbaseClient = new CoinbaseProClient();

            var socket = coinbaseClient.WebSocket;
            socket.OnMatchReceived += (sender, args) => queue.Enqueue(new Trade(args.LastOrder.TradeId, args.LastOrder.Time.Date, args.LastOrder.Price));

            Log.Debug("Starting websocket for products: {@Products}", products);
            socket.Start(products, new List<ChannelType>() { ChannelType.Matches });
            var tokenSource = new CancellationTokenSource();

            await Task.Run(() => Run(new ProducerConfig
            {
                BootstrapServers = server,
                ClientId = Dns.GetHostName(),
            }, tokenSource.Token));
        }

        private static async Task Run(ProducerConfig producerConfig, CancellationToken token)
        {
            Log.Debug("Building producer with config {@Config}", producerConfig);
            using (var producer = new ProducerBuilder<Null, string>(producerConfig).Build())
            {
                var topic = config.Get<string>("topic");
                while (!token.IsCancellationRequested)
                {
                    if (!queue.TryDequeue(out var trade))
                        continue;

                    var json = JsonSerializer.Serialize(trade, new JsonSerializerOptions() { PropertyNameCaseInsensitive = true });
                    Log.Debug("Writing trade {Trade} to topic {Topic}", json, topic);

                    var result = await producer.ProduceAsync(topic, new Message<Null, string>() { Value = json }, token);
                    if (result.Status == PersistenceStatus.Persisted)
                        Log.Debug("Successfully persisted");
                    else
                        Log.Warning("Failed to write to topic {topic}: {@Result}", topic, result);
                }
            }
        }
    }
}
