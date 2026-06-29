<template>
  <v-card>
    <v-toolbar class="elevation-0">
      <v-flex xs3>
        <v-select
          label="订单状态"
          :items="statusOptions"
          item-text="text"
          item-value="value"
          v-model="status"
          hide-details
        />
      </v-flex>
      <v-spacer/>
      <v-btn color="primary" @click="getOrderList">刷新</v-btn>
    </v-toolbar>
    <v-divider/>
    <v-data-table
      :headers="headers"
      :items="orderList"
      :pagination.sync="pagination"
      :total-items="total"
      :loading="loading"
      class="elevation-1">
      <template slot="items" slot-scope="props">
        <td class="text-xs-center">{{ props.item.orderId }}</td>
        <td class="text-xs-left">{{ $format(props.item.actualPay) }}</td>
        <td class="text-xs-left">{{ props.item.buyerNick }}</td>
        <td class="text-xs-left">{{ props.item.createTime }}</td>
        <td class="text-xs-left">{{ props.item.receiver }}</td>
        <td class="text-xs-left">{{ props.item.receiverMobile }}</td>
        <td class="text-xs-left">{{ props.item.receiverAddress }}</td>
        <td class="text-xs-left">{{ props.item.paymentType ? "在线支付" : "货到付款" }}</td>
        <td class="text-xs-left">{{ statusText(props.item.orderStatus) }}</td>
        <td class="justify-center layout px-0">
          <v-btn flat small color="primary" @click="nextStatus(props.item)" :disabled="!canNext(props.item)">
            {{ nextStatusText(props.item) }}
          </v-btn>
          <v-btn icon @click="deleteOrder(props.item)">
            <i class="el-icon-delete"/>
          </v-btn>
        </td>
      </template>
    </v-data-table>
  </v-card>
</template>

<script>
export default {
  name: "order",
  data() {
    return {
      status: 0,
      total: 0,
      loading: false,
      pagination: {},
      statusOptions: [
        { text: "全部", value: 0 },
        { text: "未付款", value: 1 },
        { text: "已付款", value: 2 },
        { text: "已发货", value: 3 },
        { text: "已完成", value: 4 },
        { text: "已关闭", value: 5 },
        { text: "已评价", value: 6 }
      ],
      headers: [
        { text: "订单号", align: "center", value: "orderId" },
        { text: "总金额（元）", value: "actualPay" },
        { text: "买家昵称", sortable: false, value: "buyerNick" },
        { text: "下单时间", value: "createTime" },
        { text: "收货人", sortable: false, value: "receiver" },
        { text: "收货电话", sortable: false, value: "receiverMobile" },
        { text: "收货地址", sortable: false, value: "receiverAddress" },
        { text: "支付类型", sortable: false, value: "paymentType" },
        { text: "订单状态", sortable: false, value: "orderStatus" },
        { text: "操作", align: "center", sortable: false }
      ],
      orderList: []
    };
  },
  mounted() {
    this.getOrderList();
  },
  watch: {
    pagination: {
      deep: true,
      handler() {
        this.getOrderList();
      }
    },
    status() {
      this.getOrderList();
    }
  },
  methods: {
    getOrderList() {
      this.loading = true;
      this.$http
        .get("/order/list", {
          params: {
            page: this.pagination.page,
            rows: this.pagination.rowsPerPage,
            status: this.status || null
          }
        })
        .then(resp => {
          this.orderList = resp.data.items;
          this.total = resp.data.total;
        })
        .catch(() => {
          this.$message.error("获取订单列表失败");
        })
        .then(() => {
          this.loading = false;
        });
    },
    statusText(status) {
      const option = this.statusOptions.find(item => item.value === status);
      return option ? option.text : "未知";
    },
    canNext(order) {
      return order.orderStatus >= 1 && order.orderStatus < 4;
    },
    nextStatusText(order) {
      const next = order.orderStatus + 1;
      if (next === 2) return "确认付款";
      if (next === 3) return "确认发货";
      if (next === 4) return "完成订单";
      return "不可操作";
    },
    nextStatus(order) {
      if (!this.canNext(order)) {
        return;
      }
      this.$http
        .put("/order/" + order.orderId + "/" + (order.orderStatus + 1))
        .then(() => {
          this.$message.success("订单状态更新成功！");
          this.getOrderList();
        })
        .catch(() => {
          this.$message.error("订单状态更新失败！");
        });
    },
    deleteOrder(order) {
      this.$message.confirm("确认删除该订单吗？").then(() => {
        this.$http
          .delete("/order/" + order.orderId)
          .then(() => {
            this.$message.success("删除成功！");
            this.getOrderList();
          })
          .catch(() => {
            this.$message.error("删除失败！");
          });
      });
    }
  }
};
</script>

<style scoped>
</style>
