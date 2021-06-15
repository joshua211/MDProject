using System;

namespace Producer
{
    public class Trade
    {
        public long TradeId { get; set; }
        public string Symbol { get; set; }
        public DateTime Time { get; set; }
        public decimal Price { get; set; }
        public decimal Size { get; set; }



        public Trade(long tradeId, DateTime time, decimal price, decimal size, string symbol)
        {
            TradeId = tradeId;
            Time = time;
            Price = price;
            Size = size;
            Symbol = symbol;
        }
    }
}