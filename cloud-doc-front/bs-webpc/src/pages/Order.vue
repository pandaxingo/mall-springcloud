<template>
  <div class="order">
    <div class="order-header">
      <div class="order-header-content">
        <p>
          <i class="el-icon-s-order"></i>
          我的订单
        </p>
      </div>
    </div>

    <div class="order-content">
      <div class="order-toolbar">
        <el-radio-group v-model="status" size="small" @change="changeStatus">
          <el-radio-button :label="0">全部</el-radio-button>
          <el-radio-button :label="1">待付款</el-radio-button>
          <el-radio-button :label="2">待发货</el-radio-button>
          <el-radio-button :label="3">待收货</el-radio-button>
          <el-radio-button :label="4">已完成</el-radio-button>
          <el-radio-button :label="5">已取消</el-radio-button>
        </el-radio-group>
        <el-button size="small" icon="el-icon-refresh" @click="loadOrders">刷新</el-button>
      </div>

      <div v-loading="loading">
        <div v-if="orders.length > 0">
          <div class="content" v-for="item in orders" :key="item.orderId">
            <div class="order-info">
              <div>
                <span class="order-id">订单编号：{{ item.orderId }}</span>
                <span class="order-time">下单时间：{{ item.createTime | dateFormat }}</span>
              </div>
              <el-tag :type="statusType(item.orderStatus)" size="small">
                {{ statusText(item.orderStatus) }}
              </el-tag>
            </div>

            <ul>
              <li class="header">
                <div class="pro-img"></div>
                <div class="pro-name">商品名称</div>
                <div class="pro-price">单价</div>
                <div class="pro-num">数量</div>
                <div class="pro-total">小计</div>
              </li>

              <li class="product-list" v-for="detail in item.orderDetails" :key="detail.id || detail.skuId">
                <div class="pro-img">
                  <router-link :to="{ path: '/goods/details', query: { productID: detail.skuId } }">
                    <img :src="$imageUrl(detail.image)" @error="$imageError" />
                  </router-link>
                </div>
                <div class="pro-name">
                  <router-link :to="{ path: '/goods/details', query: { productID: detail.skuId } }">
                    {{ shortTitle(detail.title) }}
                  </router-link>
                </div>
                <div class="pro-price">{{ money(detail.price) }}元</div>
                <div class="pro-num">{{ detail.num }}</div>
                <div class="pro-total pro-total-in">{{ money(lineTotal(detail)) }}元</div>
              </li>
            </ul>

            <div class="order-bar">
              <div class="order-bar-left">
                共 <span class="order-total-num">{{ countItems(item) }}</span> 件商品
                <span class="receiver">收货人：{{ item.receiver || '-' }}</span>
              </div>
              <div class="order-bar-right">
                <span class="total-price-title">合计：</span>
                <span class="total-price">{{ money(item.actualPay || item.totalPay) }}元</span>
                <el-button size="small" @click="showDetail(item.orderId)">查看详情</el-button>
                <el-button
                  v-if="canPay(item)"
                  size="small"
                  type="primary"
                  @click="payOrder(item.orderId)"
                >去付款</el-button>
                <el-button
                  v-if="canCancel(item)"
                  size="small"
                  type="warning"
                  @click="cancelOrder(item.orderId)"
                >取消订单</el-button>
                <el-button
                  v-if="canReceive(item)"
                  size="small"
                  type="success"
                  @click="receiveOrder(item.orderId)"
                >确认收货</el-button>
                <el-button
                  v-if="canDelete(item)"
                  size="small"
                  type="danger"
                  @click="deleteOrder(item.orderId)"
                >删除</el-button>
              </div>
            </div>
          </div>

          <div class="pager" v-if="total > rows">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="page"
              :page-size="rows"
              :total="total"
              @current-change="changePage"
            />
          </div>
        </div>

        <div v-else class="order-empty">
          <div class="empty">
            <h2>暂无订单</h2>
            <p>去挑选喜欢的商品吧。</p>
            <router-link to="/goods">
              <el-button type="primary">去购物</el-button>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <el-dialog title="订单详情" :visible.sync="detailVisible" width="760px">
      <div v-if="currentOrder" class="detail">
        <p>订单编号：{{ currentOrder.orderId }}</p>
        <p>订单状态：{{ statusText(currentOrder.orderStatus) }}</p>
        <p>收货信息：{{ currentOrder.receiver }}，{{ currentOrder.receiverMobile }}，{{ receiverAddress(currentOrder) }}</p>
        <el-table :data="currentOrder.orderDetails || []" size="small" style="width: 100%; margin-top: 15px;">
          <el-table-column label="图片" width="90">
            <template slot-scope="scope">
              <img class="detail-img" :src="$imageUrl(scope.row.image)" @error="$imageError" />
            </template>
          </el-table-column>
          <el-table-column prop="title" label="商品名称" />
          <el-table-column label="单价" width="100">
            <template slot-scope="scope">{{ money(scope.row.price) }}元</template>
          </el-table-column>
          <el-table-column prop="num" label="数量" width="80" />
          <el-table-column label="小计" width="110">
            <template slot-scope="scope">{{ money(lineTotal(scope.row)) }}元</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: "Order",
  data() {
    return {
      orders: [],
      loading: false,
      page: 1,
      rows: 5,
      total: 0,
      status: 0,
      detailVisible: false,
      currentOrder: null
    };
  },
  created() {
    this.loadOrders();
    this.syncPayResult();
  },
  activated() {
    this.loadOrders();
    this.syncPayResult();
  },
  methods: {
    showAuthError(error) {
      if (error.response && error.response.status === 401) {
        this.notifyError("登录已失效，请重新登录");
        this.$store.dispatch("setShowLogin", true);
        this.$router.push("/");
        return true;
      }
      return false;
    },
    loadOrders() {
      this.loading = true;
      const params = {
        page: this.page,
        rows: this.rows
      };
      if (this.status) {
        params.status = this.status;
      }
      this.$http
        .get("/order/list", { params })
        .then(res => {
          const data = res.data || {};
          this.orders = Array.isArray(data.items) ? data.items : [];
          this.total = Number(data.total || this.orders.length);
        })
        .catch(error => {
          this.orders = [];
          this.total = 0;
          if (!this.showAuthError(error)) {
            this.notifyError("订单加载失败，请确认订单服务已启动");
          }
        })
        .finally(() => {
          this.loading = false;
        });
    },
    changeStatus() {
      this.page = 1;
      this.loadOrders();
    },
    changePage(page) {
      this.page = page;
      this.loadOrders();
    },
    showDetail(orderId) {
      this.$http.get(`/order/${orderId}`).then(res => {
        this.currentOrder = res.data;
        this.detailVisible = true;
      }).catch(error => {
        if (!this.showAuthError(error)) {
          this.notifyError("订单详情加载失败");
        }
      });
    },
    payOrder(orderId) {
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
    syncPayResult() {
      const orderId = window.localStorage.getItem("lastPayOrderId");
      if (!orderId) {
        return;
      }
      this.$http.post("/order/tradeQuery", null, {
        params: { orderId }
      }).then(() => {
        window.localStorage.removeItem("lastPayOrderId");
        this.notifySucceed("支付状态已同步");
        this.loadOrders();
      }).catch(() => {
        this.loadOrders();
      });
    },
    cancelOrder(orderId) {
      this.$confirm("确定取消该订单吗？取消后会恢复商品库存。", "提示", {
        type: "warning"
      }).then(() => {
        this.$http.put(`/order/${orderId}/5`).then(() => {
          this.notifySucceed("订单已取消");
          this.loadOrders();
        }).catch(error => {
          if (!this.showAuthError(error)) {
            this.notifyError("取消订单失败");
          }
        });
      }).catch(() => {});
    },
    receiveOrder(orderId) {
      this.$confirm("确认已经收到商品了吗？", "提示", {
        type: "warning"
      }).then(() => {
        this.$http.put(`/order/${orderId}/4`).then(() => {
          this.notifySucceed("确认收货成功");
          this.loadOrders();
        }).catch(error => {
          if (!this.showAuthError(error)) {
            this.notifyError("确认收货失败");
          }
        });
      }).catch(() => {});
    },
    deleteOrder(orderId) {
      this.$confirm("确定删除该订单记录吗？", "提示", {
        type: "warning"
      }).then(() => {
        this.$http.delete(`/order/${orderId}`).then(() => {
          this.notifySucceed("订单已删除");
          this.loadOrders();
        }).catch(error => {
          if (!this.showAuthError(error)) {
            this.notifyError("删除订单失败");
          }
        });
      }).catch(() => {});
    },
    canPay(order) {
      return Number(order.orderStatus) === 1;
    },
    canCancel(order) {
      return Number(order.orderStatus) === 1;
    },
    canReceive(order) {
      return Number(order.orderStatus) === 3;
    },
    canDelete(order) {
      return [1, 4, 5, 6].indexOf(Number(order.orderStatus)) !== -1;
    },
    statusText(status) {
      const map = {
        1: "待付款",
        2: "待发货",
        3: "待收货",
        4: "已完成",
        5: "已取消",
        6: "已评价"
      };
      return map[Number(status)] || "未知";
    },
    statusType(status) {
      const map = {
        1: "warning",
        2: "",
        3: "success",
        4: "success",
        5: "info",
        6: "info"
      };
      return map[Number(status)] || "info";
    },
    shortTitle(title) {
      if (!title) {
        return "";
      }
      return title.length > 26 ? title.substring(0, 26) + "..." : title;
    },
    lineTotal(detail) {
      return Number(detail.price || 0) * Number(detail.num || 0);
    },
    countItems(order) {
      return (order.orderDetails || []).reduce((sum, detail) => {
        return sum + Number(detail.num || 0);
      }, 0);
    },
    money(value) {
      const number = Number(value || 0);
      return number.toFixed(2).replace(/\.00$/, "");
    },
    receiverAddress(order) {
      return [
        order.receiverState,
        order.receiverCity,
        order.receiverDistrict,
        order.receiverAddress
      ].filter(Boolean).join(" ");
    }
  }
};
</script>

<style scoped>
.order {
  background-color: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 30px;
}
.order-header {
  height: 64px;
  border-bottom: 2px solid #ff6700;
  background-color: #fff;
  margin-bottom: 20px;
}
.order-header-content {
  width: 1225px;
  margin: 0 auto;
}
.order-header p {
  font-size: 28px;
  line-height: 58px;
  float: left;
  font-weight: normal;
  color: #424242;
}
.order-header i {
  color: #ff6700;
  font-size: 30px;
  margin-right: 8px;
}
.order-content {
  width: 1225px;
  margin: 0 auto;
}
.order-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 16px;
  background-color: #fff;
}
.content {
  background-color: #fff;
  margin-bottom: 20px;
}
.order-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 26px;
  color: #424242;
  border-bottom: 1px solid #ff6700;
}
.order-id {
  color: #ff6700;
  margin-right: 30px;
}
.order-time {
  color: #757575;
}
.content ul {
  background-color: #fff;
  color: #424242;
  line-height: 85px;
}
.header {
  height: 85px;
  padding-right: 26px;
  color: #424242;
}
.product-list {
  height: 85px;
  padding: 15px 26px 15px 0;
  border-top: 1px solid #e0e0e0;
}
.pro-img {
  float: left;
  height: 85px;
  width: 120px;
  padding-left: 80px;
}
.pro-img img {
  height: 80px;
  width: 80px;
  object-fit: cover;
}
.pro-name {
  float: left;
  width: 420px;
}
.pro-name a {
  color: #424242;
}
.pro-name a:hover {
  color: #ff6700;
}
.pro-price {
  float: left;
  width: 160px;
  padding-right: 18px;
  text-align: center;
}
.pro-num {
  float: left;
  width: 160px;
  text-align: center;
}
.pro-total {
  float: left;
  width: 160px;
  padding-right: 70px;
  text-align: right;
}
.pro-total-in {
  color: #ff6700;
}
.order-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 58px;
  padding: 0 20px;
  border-top: 1px solid #ff6700;
  background-color: #fff;
}
.order-bar-left {
  color: #757575;
}
.order-total-num,
.total-price-title,
.total-price {
  color: #ff6700;
}
.receiver {
  margin-left: 24px;
}
.order-bar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.total-price {
  font-size: 28px;
  margin-right: 10px;
}
.pager {
  text-align: center;
  padding: 10px 0 30px;
}
.order-empty {
  background-color: #fff;
}
.empty {
  min-height: 300px;
  padding: 70px 0 90px 558px;
  background: url(../assets/image/cart-empty.png) no-repeat 124px 55px;
  color: #b0b0b0;
}
.empty h2 {
  margin: 0 0 15px;
  font-size: 36px;
  font-weight: normal;
}
.empty p {
  margin: 0 0 20px;
  font-size: 20px;
}
.detail p {
  line-height: 26px;
  color: #424242;
}
.detail-img {
  width: 58px;
  height: 58px;
  object-fit: cover;
}
</style>
