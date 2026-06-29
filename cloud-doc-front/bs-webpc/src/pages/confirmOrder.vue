<template>
  <div class="confirmOrder">
    <div class="confirmOrder-header">
      <div class="header-content">
        <p><i class="el-icon-s-order"></i></p>
        <p>确认订单</p>
        <router-link to></router-link>
      </div>
    </div>

    <div class="content">
      <div class="section-address">
        <p class="title">收货地址</p>
        <div class="address-body">
          <ul>
            <li
              v-for="item in address"
              :key="item.id"
              :class="item.id == confirmAddress ? 'in-section' : ''"
              @click="confirmAddress = item.id"
            >
              <h2>收货人：{{ item.name }}</h2>
              <p class="phone">联系电话：{{ item.phone }}</p>
              <p class="address">所在城市：{{ item.province }} {{ item.city }} {{ item.district }}</p>
              <p class="address">详细地址：{{ item.address }}</p>
              <p class="default">默认地址：{{ item.defaultAddress ? '是' : '备选地址' }}</p>
            </li>
            <li class="add-address" @click="$router.push('/address')">
              <i class="el-icon-circle-plus-outline"></i>
              <p>添加新地址</p>
            </li>
          </ul>
        </div>
      </div>

      <div class="section-goods">
        <p class="title">商品及优惠券</p>
        <div class="goods-list">
          <ul>
            <li v-for="item in orders" :key="item.id">
              <img :src="$imageUrl(item.image)" @error="$imageError" />
              <span class="pro-name">{{ item.title }}</span>
              <span class="pro-price">{{ priceYuan(item.price) }}元 x {{ item.num }}</span>
              <span class="pro-status">待支付</span>
              <span class="pro-total">{{ priceYuan(item.price * item.num) }}元</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="section-shipment">
        <p class="title">配送方式</p>
        <p class="shipment">包邮</p>
      </div>

      <div class="section-invoice">
        <p class="title">发票</p>
        <p class="invoice">电子发票</p>
        <p class="invoice">个人</p>
        <p class="invoice">商品明细</p>
      </div>

      <div class="section-count">
        <div class="money-box">
          <ul>
            <li>
              <span class="title">商品件数：</span>
              <span class="value">{{ counts }}件</span>
            </li>
            <li>
              <span class="title">商品总价：</span>
              <span class="value">{{ priceYuan(totals) }}元</span>
            </li>
            <li>
              <span class="title">运费：</span>
              <span class="value">{{ priceYuan(postFee) }}元</span>
            </li>
            <li class="total">
              <span class="title">应付总额：</span>
              <span class="value"><span class="total-price">{{ priceYuan(payTotal) }}</span>元</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="section-bar">
        <div class="btn">
          <router-link to="/shoppingCart" class="btn-base btn-return">返回购物车</router-link>
          <a href="javascript:void(0);" @click="addOrder" class="btn-base btn-primary">结算</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { mapActions } from "vuex";

export default {
  name: "ConfirmOrder",
  data() {
    return {
      confirmAddress: null,
      orders: [],
      counts: 0,
      totals: 0,
      postFee: 0,
      htmls: "",
      address: [],
      orderIds: []
    };
  },
  created() {
    this.getAddress();
    this.getPreOrder();
  },
  methods: {
    ...mapActions(["deleteShoppingCart"]),
    addOrder() {
      const selectedAddress = this.address.find(item => item.id == this.confirmAddress) || this.address[0];
      if (!selectedAddress) {
        this.notifyError("请先添加收货地址");
        this.$router.push("/address");
        return;
      }

      const order = {
        totalPay: this.totals,
        postFee: this.postFee,
        paymentType: true,
        actualPay: this.payTotal,
        buyerMessage: "测试IT",
        buyerNick: "Cloud",
        orderDetails: (this.orders || []).map(item => ({
          skuId: item.skuId,
          num: Number(item.num || 1),
          title: item.title,
          ownSpec: item.ownSpec,
          price: Number(item.price || 0),
          image: item.image
        })),
        receiver: selectedAddress.name,
        receiverMobile: selectedAddress.phone,
        receiverState: selectedAddress.province || "",
        receiverCity: selectedAddress.city,
        receiverDistrict: selectedAddress.district,
        receiverAddress: selectedAddress.address,
        receiverZip: selectedAddress.zipcode,
        invoiceType: 0,
        sourceType: 2
      };

      this.$http.post("/order", order).then(res => {
        if (res.status !== 201) {
          this.notifyError("商品库存不足，无法创建订单");
          return;
        }
        this.orderIds = res.data;
        const orderId = Array.isArray(res.data) ? res.data[0] : res.data;
        if (!orderId) {
          this.notifyError("订单创建失败，未获取到订单号");
          return;
        }
        window.localStorage.removeItem("BS_BUY_NOW");
        this.openAlipay(orderId);
        this.notifySucceed("订单已经生成，正在跳转支付宝沙盒支付");
      }).catch(error => {
        console.error("订单创建失败：", error && error.response ? error.response : error);
        const message = error && error.response && error.response.data
          ? error.response.data.message || error.response.data
          : "订单创建失败，请稍后再试";
        this.notifyError(message);
      });
    },
    openAlipay(orderId) {
      window.localStorage.setItem("lastPayOrderId", orderId);
      const route = this.$router.resolve({
        path: "/alipay",
        query: { orderId }
      });
      const payWindow = window.open(route.href, "_blank");
      if (!payWindow) {
        this.$router.push({ path: "/alipay", query: { orderId } });
      }
    },
    getAddress() {
      this.$http
        .get("/order/address")
        .then(res => {
          this.address = Array.isArray(res.data) ? res.data : [];
          const defaultAddress = this.address.find(item => item.defaultAddress);
          this.confirmAddress = defaultAddress ? defaultAddress.id : (this.address[0] && this.address[0].id);
        })
        .catch(error => {
          if (error.response && error.response.status === 404) {
            this.address = [];
            this.confirmAddress = null;
            return;
          }
          this.notifyError("收货地址加载失败");
        });
    },
    getPreOrder() {
      if (this.$route.query.buyNow) {
        const buyNow = window.localStorage.getItem("BS_BUY_NOW");
        const item = buyNow ? JSON.parse(buyNow) : null;
        this.orders = item ? [item] : [];
        this.refreshOrderPrices();
        if (this.orders.length < 1) {
          this.notifyError("请先选择商品后再结算");
          this.$router.push({ path: "/shoppingCart" });
        }
        return;
      }
      this.$http.get("/cart/find").then(res => {
        this.orders = res.data || [];
        if (this.orders.length < 1) {
          this.notifyError("请先选择商品后再结算");
          this.$router.push({ path: "/shoppingCart" });
          return;
        }
        this.refreshOrderPrices();
      });
    },
    refreshOrderPrices() {
      const tasks = (this.orders || []).map(item => {
        const skuId = item.skuId || item.id;
        if (!skuId) {
          return Promise.resolve(item);
        }
        return this.$http.get("/item/sku/" + skuId).then(resp => {
          const sku = resp.data || {};
          item.skuId = sku.id || skuId;
          item.title = sku.title || item.title;
          item.ownSpec = sku.ownSpec || item.ownSpec;
          item.price = Number(sku.price || item.price || 0);
          if (sku.images) {
            item.image = String(sku.images).split(",")[0];
          }
          if (sku.stock !== undefined && sku.stock !== null) {
            item.stock = sku.stock;
          }
          return item;
        }).catch(() => item);
      });
      Promise.all(tasks).then(() => {
        this.calcOrderTotal();
      });
    },
    calcOrderTotal() {
      let count = 0;
      let price = 0;
      this.orders.forEach(item => {
        count += Number(item.num || 1);
        price += Number(item.num || 1) * Number(item.price || 0);
      });
      this.counts = count;
      this.totals = price;
    },
    priceYuan(value) {
      return Number(value || 0).toFixed(2).replace(/\.00$/, "");
    }
  },
  computed: {
    ...mapGetters(["getCheckNum", "getTotalPrice", "getCheckGoods"]),
    payTotal() {
      return Number(this.totals || 0) + Number(this.postFee || 0);
    }
  }
};
</script>
<style scoped>
.confirmOrder {
  background-color: #f5f5f5;
  padding-bottom: 20px;
}
/* 澶撮儴CSS */
.confirmOrder .confirmOrder-header {
  background-color: #fff;
  border-bottom: 2px solid #ff6700;
  margin-bottom: 20px;
}
.confirmOrder .confirmOrder-header .header-content {
  width: 1225px;
  margin: 0 auto;
  height: 80px;
}
.confirmOrder .confirmOrder-header .header-content p {
  float: left;
  font-size: 28px;
  line-height: 80px;
  color: #424242;
  margin-right: 20px;
}
.confirmOrder .confirmOrder-header .header-content p i {
  font-size: 45px;
  color: #ff6700;
  line-height: 80px;
}
/* 澶撮儴CSS END */

/* 涓昏鍐呭瀹瑰櫒CSS */
.confirmOrder .content {
  width: 1225px;
  margin: 0 auto;
  padding: 48px 0 0;
  background-color: #fff;
}

/* 閫夋嫨鍦板潃CSS */
.confirmOrder .content .section-address {
  margin: 0 48px;
  overflow: hidden;
}
.confirmOrder .content .section-address .title {
  color: #333;
  font-size: 18px;
  line-height: 20px;
  margin-bottom: 20px;
}
.confirmOrder .content .address-body li {
  float: left;
  color: #333;
  width: 220px;
  height: 178px;
  border: 1px solid #e0e0e0;
  padding: 15px 24px 0;
  margin-right: 17px;
  margin-bottom: 24px;
}
.confirmOrder .content .address-body .in-section {
  border: 1px solid #ff6700;
}
.confirmOrder .content .address-body li h2 {
  font-size: 18px;
  font-weight: normal;
  line-height: 30px;
  margin-bottom: 10px;
}
.confirmOrder .content .address-body li p {
  font-size: 14px;
  color: #757575;
}
.confirmOrder .content .address-body li .address {
  padding: 10px 0;
  max-width: 180px;
  max-height: 88px;
  line-height: 22px;
  overflow: hidden;
}
.confirmOrder .content .address-body .add-address {
  text-align: center;
  line-height: 30px;
}
.confirmOrder .content .address-body .add-address i {
  font-size: 30px;
  padding-top: 50px;
  text-align: center;
}
/* 閫夋嫨鍦板潃CSS END */

/* 鍟嗗搧鍙婁紭鎯犲埜CSS */
.confirmOrder .content .section-goods {
  margin: 0 48px;
}
.confirmOrder .content .section-goods p.title {
  color: #333;
  font-size: 18px;
  line-height: 40px;
}
.confirmOrder .content .section-goods .goods-list {
  padding: 5px 0;
  border-top: 1px solid #e0e0e0;
  border-bottom: 1px solid #e0e0e0;
}
.confirmOrder .content .section-goods .goods-list li {
  padding: 10px 0;
  color: #424242;
  overflow: hidden;
}
.confirmOrder .content .section-goods .goods-list li img {
  float: left;
  width: 30px;
  height: 30px;
  margin-right: 10px;
}
.confirmOrder .content .section-goods .goods-list li .pro-name {
  float: left;
  width: 650px;
  line-height: 30px;
}
.confirmOrder .content .section-goods .goods-list li .pro-price {
  float: left;
  width: 150px;
  text-align: center;
  line-height: 30px;
}
.confirmOrder .content .section-goods .goods-list li .pro-status {
  float: left;
  width: 99px;
  height: 30px;
  text-align: center;
  line-height: 30px;
}
.confirmOrder .content .section-goods .goods-list li .pro-total {
  float: left;
  width: 190px;
  text-align: center;
  color: #ff6700;
  line-height: 30px;
}
/* 鍟嗗搧鍙婁紭鎯犲埜CSS END */

/* 閰嶉€佹柟寮廋SS */
.confirmOrder .content .section-shipment {
  margin: 0 48px;
  padding: 25px 0;
  border-bottom: 1px solid #e0e0e0;
  overflow: hidden;
}
.confirmOrder .content .section-shipment .title {
  float: left;
  width: 150px;
  color: #333;
  font-size: 18px;
  line-height: 38px;
}
.confirmOrder .content .section-shipment .shipment {
  float: left;
  line-height: 38px;
  font-size: 14px;
  color: #ff6700;
}
/* 閰嶉€佹柟寮廋SS END */

/* 鍙戠エCSS */
.confirmOrder .content .section-invoice {
  margin: 0 48px;
  padding: 25px 0;
  border-bottom: 1px solid #e0e0e0;
  overflow: hidden;
}
.confirmOrder .content .section-invoice .title {
  float: left;
  width: 150px;
  color: #333;
  font-size: 18px;
  line-height: 38px;
}
.confirmOrder .content .section-invoice .invoice {
  float: left;
  line-height: 38px;
  font-size: 14px;
  margin-right: 20px;
  color: #ff6700;
}
/* 鍙戠エCSS END */

/* 缁撶畻鍒楄〃CSS */
.confirmOrder .content .section-count {
  margin: 0 48px;
  padding: 20px 0;
  overflow: hidden;
}
.confirmOrder .content .section-count .money-box {
  float: right;
  text-align: right;
}
.confirmOrder .content .section-count .money-box .title {
  float: left;
  width: 126px;
  height: 30px;
  display: block;
  line-height: 30px;
  color: #757575;
}
.confirmOrder .content .section-count .money-box .value {
  float: left;
  min-width: 105px;
  height: 30px;
  display: block;
  line-height: 30px;
  color: #ff6700;
}
.confirmOrder .content .section-count .money-box .total .title {
  padding-top: 15px;
}
.confirmOrder .content .section-count .money-box .total .value {
  padding-top: 10px;
}
.confirmOrder .content .section-count .money-box .total-price {
  font-size: 30px;
}
/* 缁撶畻鍒楄〃CSS END */

/* 缁撶畻瀵艰埅CSS */
.confirmOrder .content .section-bar {
  padding: 20px 48px;
  border-top: 2px solid #f5f5f5;
  overflow: hidden;
}
.confirmOrder .content .section-bar .btn {
  float: right;
}
.confirmOrder .content .section-bar .btn .btn-base {
  float: left;
  margin-left: 30px;
  width: 158px;
  height: 38px;
  border: 1px solid #b0b0b0;
  font-size: 14px;
  line-height: 38px;
  text-align: center;
}
.confirmOrder .content .section-bar .btn .btn-return {
  color: rgba(0, 0, 0, 0.27);
  border-color: rgba(0, 0, 0, 0.27);
}
.confirmOrder .content .section-bar .btn .btn-primary {
  background: #ff6700;
  border-color: #ff6700;
  color: #fff;
}
/* 缁撶畻瀵艰埅CSS */

/* 涓昏鍐呭瀹瑰櫒CSS END */
</style>


