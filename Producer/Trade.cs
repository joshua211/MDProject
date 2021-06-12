using System;

namespace Producer
{
    public class Trade
    {
        public long TradeId { get; set; }
        public DateTime Time { get; set; }
        public decimal Price { get; set; }

        public Trade(long tradeId, DateTime time, decimal price)
        {
            TradeId = tradeId;
            Time = time;
            Price = price;
        }
    }
}