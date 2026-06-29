<template>
  <div class="address-page">
    <div class="address-header">
      <div class="address-header-content">
        <p>
          <i class="el-icon-location"></i>
          收货地址
        </p>
        <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增地址</el-button>
      </div>
    </div>

    <div class="address-content">
      <div class="address-list" v-if="addressList.length > 0">
        <div class="address-card" v-for="item in addressList" :key="item.id">
          <div class="card-main">
            <div class="card-title">
              <span>{{ item.name }}</span>
              <span class="phone">{{ item.phone }}</span>
              <el-tag v-if="item.defaultAddress" size="mini" type="success">默认</el-tag>
            </div>
            <div class="card-address">
              {{ item.province }} {{ item.city }} {{ item.district }} {{ item.address }}
            </div>
            <div class="card-zipcode" v-if="item.zipcode">邮编：{{ item.zipcode }}</div>
          </div>
          <div class="card-actions">
            <el-button
              v-if="!item.defaultAddress"
              type="text"
              @click="setDefault(item)"
            >设为默认</el-button>
            <el-button type="text" @click="openEdit(item)">编辑</el-button>
            <el-button type="text" class="delete-btn" @click="removeAddress(item)">删除</el-button>
          </div>
        </div>
      </div>

      <div class="address-empty" v-else>
        <i class="el-icon-location-outline"></i>
        <p>还没有收货地址</p>
        <el-button type="primary" @click="openAdd">新增地址</el-button>
      </div>
    </div>

    <el-dialog
      :title="form.id ? '编辑地址' : '新增地址'"
      :visible.sync="dialogVisible"
      width="520px"
      @closed="resetForm"
    >
      <el-form ref="addressForm" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="name">
          <el-input v-model.trim="form.name" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model.trim="form.province" maxlength="30" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model.trim="form.city" maxlength="30" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model.trim="form.district" maxlength="30" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model.trim="form.address" type="textarea" :rows="3" maxlength="120" />
        </el-form-item>
        <el-form-item label="邮编">
          <el-input v-model.trim="form.zipcode" maxlength="12" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.defaultAddress">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAddress">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
const emptyForm = () => ({
  id: null,
  name: "",
  phone: "",
  province: "",
  city: "",
  district: "",
  address: "",
  zipcode: "",
  defaultAddress: false
});

export default {
  name: "Address",
  data() {
    return {
      addressList: [],
      dialogVisible: false,
      saving: false,
      form: emptyForm(),
      rules: {
        name: [{ required: true, message: "请输入收货人", trigger: "blur" }],
        phone: [{ required: true, message: "请输入手机号", trigger: "blur" }],
        province: [{ required: true, message: "请输入省份", trigger: "blur" }],
        city: [{ required: true, message: "请输入城市", trigger: "blur" }],
        district: [{ required: true, message: "请输入区县", trigger: "blur" }],
        address: [{ required: true, message: "请输入详细地址", trigger: "blur" }]
      }
    };
  },
  created() {
    this.getAddressList();
  },
  methods: {
    showSuccess(message) {
      this.notifySucceed ? this.notifySucceed(message) : this.$message.success(message);
    },
    showError(message) {
      this.notifyError ? this.notifyError(message) : this.$message.error(message);
    },
    handleAuthError(error) {
      if (error.response && error.response.status === 401) {
        this.showError("登录已失效，请重新登录");
        this.$store.dispatch("setShowLogin", true);
        this.$router.push("/");
        return true;
      }
      return false;
    },
    getAddressList() {
      this.$http
        .get("/order/address")
        .then(res => {
          this.addressList = Array.isArray(res.data) ? res.data : [];
        })
        .catch(error => {
          if (this.handleAuthError(error)) {
            return;
          }
          if (error.response && error.response.status === 404) {
            this.addressList = [];
            return;
          }
          this.showError("地址加载失败");
        });
    },
    openAdd() {
      this.form = emptyForm();
      this.dialogVisible = true;
    },
    openEdit(item) {
      this.form = Object.assign(emptyForm(), item);
      this.dialogVisible = true;
    },
    resetForm() {
      this.form = emptyForm();
      if (this.$refs.addressForm) {
        this.$refs.addressForm.clearValidate();
      }
    },
    saveAddress() {
      this.$refs.addressForm.validate(valid => {
        if (!valid) {
          return;
        }
        this.saving = true;
        const request = this.form.id
          ? this.$http.put("/order/address", this.form)
          : this.$http.post("/order/address", this.form);
        request
          .then(() => {
            this.dialogVisible = false;
            this.showSuccess("地址保存成功");
            this.getAddressList();
          })
          .catch(error => {
            if (!this.handleAuthError(error)) {
              this.showError("地址保存失败");
            }
          })
          .finally(() => {
            this.saving = false;
          });
      });
    },
    setDefault(item) {
      const data = Object.assign({}, item, { defaultAddress: true });
      this.$http
        .put("/order/address", data)
        .then(() => {
          this.showSuccess("已设为默认地址");
          this.getAddressList();
        })
        .catch(error => {
          if (!this.handleAuthError(error)) {
            this.showError("设置失败");
          }
        });
    },
    removeAddress(item) {
      this.$confirm("确定删除这个收货地址吗？", "提示", {
        type: "warning"
      })
        .then(() => this.$http.delete("/order/address/" + item.id))
        .then(() => {
          this.addressList = this.addressList.filter(address => address.id !== item.id);
          this.showSuccess("地址已删除");
          this.getAddressList();
        })
        .catch(error => {
          if (error === "cancel" || error === "close") {
            return;
          }
          if (!this.handleAuthError(error)) {
            this.showError("删除失败");
          }
        });
    }
  }
};
</script>

<style scoped>
.address-page {
  min-height: 640px;
  background: #f5f5f5;
  padding-bottom: 30px;
}
.address-header {
  background: #fff;
  border-bottom: 2px solid #ff6700;
  margin-bottom: 20px;
}
.address-header-content {
  width: 1225px;
  height: 80px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.address-header-content p {
  font-size: 26px;
  color: #424242;
}
.address-header-content i {
  color: #ff6700;
  font-size: 30px;
  margin-right: 8px;
}
.address-content {
  width: 1225px;
  margin: 0 auto;
  background: #fff;
  min-height: 420px;
  padding: 30px;
  box-sizing: border-box;
}
.address-card {
  border: 1px solid #e0e0e0;
  padding: 20px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.address-card:hover {
  border-color: #ff6700;
}
.card-main {
  line-height: 1.8;
  color: #424242;
}
.card-title {
  font-size: 18px;
  font-weight: bold;
}
.phone {
  margin: 0 12px;
  color: #757575;
  font-weight: normal;
}
.card-address,
.card-zipcode {
  font-size: 14px;
  color: #616161;
}
.card-actions {
  min-width: 210px;
  text-align: right;
}
.delete-btn {
  color: #f56c6c;
}
.address-empty {
  height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}
.address-empty i {
  font-size: 72px;
  margin-bottom: 18px;
}
.address-empty p {
  font-size: 18px;
  margin-bottom: 20px;
}
</style>
