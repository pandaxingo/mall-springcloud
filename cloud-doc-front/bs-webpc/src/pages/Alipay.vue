<template>
  <div class="alipay-page">
    <p>{{ message }}</p>
    <div ref="payForm" v-html="apply"></div>
    <div v-if="errorText" class="error-box">
      <p>{{ errorText }}</p>
      <el-button type="primary" size="small" @click="$router.push('/order')">返回订单</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: "Alipay",
  data() {
    return {
      apply: "",
      message: "正在跳转到支付宝沙盒支付，请稍候...",
      errorText: ""
    };
  },
  mounted() {
    this.loadPayForm();
  },
  methods: {
    loadPayForm() {
      const orderId = this.$route.query.orderId;
      if (!orderId) {
        this.showError("缺少订单号，请返回订单重新发起支付");
        return;
      }

      const returnOrigin = window.location.hostname === "www.wxw.com"
        ? "http://localhost:9002"
        : window.location.origin;

      this.$http
        .get("/order/goAlipay", {
          params: {
            orderId,
            returnUrl: `${returnOrigin}/pay-return.html`
          }
        })
        .then(res => {
          this.apply = res.data || "";
          if (!this.apply || this.apply.indexOf("<form") === -1) {
            this.showError((this.apply || "支付宝支付表单生成失败") + "，当前订单号：" + orderId);
            return;
          }
          this.submitPayForm();
        })
        .catch(error => {
          const msg = error.response && error.response.data ? error.response.data : "支付宝支付表单请求失败";
          this.showError(msg + "，当前订单号：" + orderId);
        });
    },
    submitPayForm() {
      this.$nextTick(() => {
        setTimeout(() => {
          const form = this.$refs.payForm && this.$refs.payForm.querySelector("form");
          if (!form) {
            this.showError("未找到支付宝支付表单，请返回订单重新发起支付");
            return;
          }
          this.message = "正在打开支付宝沙盒付款页面...";
          form.submit();
        }, 100);
      });
    },
    showError(message) {
      this.message = "跳转支付宝沙盒失败";
      this.errorText = String(message);
      this.notifyError(this.errorText);
    }
  }
};
</script>

<style scoped>
.alipay-page {
  min-height: 280px;
  padding-top: 120px;
  text-align: center;
  color: #666;
  font-size: 18px;
  background: #f5f5f5;
}
.error-box {
  width: 640px;
  margin: 24px auto 0;
  padding: 18px;
  color: #c45656;
  font-size: 14px;
  line-height: 24px;
  background: #fff;
  border: 1px solid #fde2e2;
}
.error-box p {
  margin-bottom: 14px;
  word-break: break-all;
}
</style>
