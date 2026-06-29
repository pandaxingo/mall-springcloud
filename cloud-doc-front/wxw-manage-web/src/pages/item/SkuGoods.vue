<template>
  <v-card>
    <v-toolbar class="elevation-0">
      <v-toolbar-title>前台商品管理</v-toolbar-title>
      <v-btn color="primary" class="ml-3" @click="addSku">添加前台商品</v-btn>
      <v-spacer/>
      <v-flex xs3>
        状态：
        <v-btn-toggle mandatory v-model.lazy="filter.enable">
          <v-btn flat>全部</v-btn>
          <v-btn flat :value="true">启用</v-btn>
          <v-btn flat :value="false">停用</v-btn>
        </v-btn-toggle>
      </v-flex>
      <v-flex xs3>
        <v-text-field append-icon="search" label="搜索SKU标题" single-line hide-details v-model="filter.search"/>
      </v-flex>
    </v-toolbar>
    <v-divider/>

    <v-data-table
      :headers="headers"
      :items="skuList"
      :pagination.sync="pagination"
      :total-items="total"
      :loading="loading"
      class="elevation-1">
      <template slot="items" slot-scope="props">
        <td class="text-xs-center">{{ props.item.id }}</td>
        <td class="text-xs-center">{{ props.item.spuId }}</td>
        <td class="text-xs-center">{{ areaName(props.item) }}</td>
        <td class="text-xs-center">{{ linkedSpuName(props.item.spuId) }}</td>
        <td class="text-xs-center">{{ props.item.title }}</td>
        <td class="text-xs-center">{{ formatPrice(props.item.price) }}</td>
        <td class="text-xs-center">{{ props.item.stock || 0 }}</td>
        <td class="text-xs-center">
          <img class="sku-img" :src="firstImage(props.item.images)" v-if="firstImage(props.item.images)">
          <span v-else>无</span>
        </td>
        <td class="text-xs-center">
          <v-chip small :color="props.item.enable ? 'success' : 'grey'" text-color="white">
            {{ props.item.enable ? '启用' : '停用' }}
          </v-chip>
        </td>
        <td class="justify-center layout px-0">
          <v-btn icon @click="editSku(props.item)">
            <i class="el-icon-edit"/>
          </v-btn>
          <v-btn icon @click="deleteSku(props.item)">
            <i class="el-icon-delete"/>
          </v-btn>
          <v-btn flat small color="warning" v-if="props.item.enable" @click="updateEnable(props.item, false)">停用</v-btn>
          <v-btn flat small color="success" v-else @click="updateEnable(props.item, true)">启用</v-btn>
        </td>
      </template>
    </v-data-table>

    <v-dialog max-width="820" v-model="show" persistent scrollable>
      <v-card>
        <v-toolbar dense dark color="primary">
          <v-toolbar-title>{{ skuForm.id ? '编辑前台商品' : '添加前台商品' }}</v-toolbar-title>
          <v-spacer/>
          <v-btn icon @click="show=false">
            <v-icon>close</v-icon>
          </v-btn>
        </v-toolbar>
        <v-card-text>
          <v-form ref="skuForm">
            <v-select
              :items="areaOptions"
              item-text="text"
              item-value="value"
              label="首页展示区域"
              v-model="skuForm.homeArea"
              required
              :rules="[v => !!v || '请选择首页展示区域']"/>
            <v-select
              :items="spuOptions"
              item-text="title"
              item-value="id"
              label="关联商品列表商品"
              v-model="skuForm.spuId"
              required
              autocomplete
              clearable
              :rules="[v => !!v || '请选择关联商品']"/>
            <v-text-field label="首页商品标题" v-model="skuForm.title" required :rules="[v => !!v || '标题不能为空']"/>
            <v-layout row>
              <v-flex xs5>
                <v-text-field label="价格（元）" type="number" v-model="skuForm.priceYuan" required/>
              </v-flex>
              <v-spacer/>
              <v-flex xs5>
                <v-text-field label="库存" type="number" v-model="skuForm.stock" required/>
              </v-flex>
            </v-layout>
            <v-switch label="启用" v-model="skuForm.enable"/>

            <div class="subheading mb-2">首页展示图片</div>
            <div class="image-list">
              <div class="image-item" v-for="(img, index) in skuForm.imageList" :key="img">
                <img :src="normalizeImageUrl(img)">
                <v-btn small icon class="remove-image" @click="removeImage(index)">
                  <v-icon small>close</v-icon>
                </v-btn>
              </div>
              <label class="upload-box">
                <input type="file" accept="image/*" @change="uploadSkuImage">
                <v-icon>add</v-icon>
              </label>
            </div>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer/>
          <v-btn flat @click="show=false">取消</v-btn>
          <v-btn color="primary" @click="saveSku">保存</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>

<script>
import config from "../../config";

const emptySkuForm = () => ({
  id: null,
  spuId: null,
  homeArea: "vegetables",
  title: "",
  priceYuan: 0,
  stock: 100,
  enable: true,
  imageList: []
});

export default {
  name: "sku-goods",
  data() {
    return {
      filter: {
        enable: true,
        search: ""
      },
      pagination: {},
      loading: false,
      total: 0,
      skuList: [],
      spuOptions: [],
      show: false,
      skuForm: emptySkuForm(),
      areaOptions: [
        { text: "畅销蔬菜区域", value: "vegetables" },
        { text: "新鲜水果区域", value: "fruits" },
        { text: "热销农机区域", value: "machinery" }
      ],
      headers: [
        { text: "SKUID", align: "center", value: "id" },
        { text: "关联SPU", align: "center", value: "spuId" },
        { text: "首页区域", align: "center", sortable: false },
        { text: "关联商品列表商品", align: "center", sortable: false },
        { text: "首页标题", align: "center", sortable: false, value: "title" },
        { text: "价格", align: "center", value: "price" },
        { text: "库存", align: "center", value: "stock" },
        { text: "图片", align: "center", sortable: false },
        { text: "状态", align: "center", value: "enable" },
        { text: "操作", align: "center", sortable: false }
      ]
    };
  },
  mounted() {
    this.loadSpuOptions();
    this.getDataFromServer();
  },
  watch: {
    pagination: {
      deep: true,
      handler() {
        this.getDataFromServer();
      }
    },
    filter: {
      deep: true,
      handler() {
        this.getDataFromServer();
      }
    }
  },
  methods: {
    getDataFromServer() {
      this.loading = true;
      this.$http.get("/item/sku/page", {
        params: {
          key: this.filter.search,
          page: this.pagination.page,
          rows: this.pagination.rowsPerPage,
          enable: this.filter.enable === 0 ? null : this.filter.enable,
          homeOnly: true
        }
      }).then(resp => {
        this.skuList = resp.data.items || [];
        this.total = resp.data.total || 0;
        this.loading = false;
      }).catch(() => {
        this.loading = false;
        this.$message.error("加载前台商品失败");
      });
    },
    loadSpuOptions() {
      this.$http.get("/item/spu/page", {
        params: {
          page: 1,
          rows: 200,
          saleable: true
        }
      }).then(resp => {
        this.spuOptions = resp.data.items || [];
      }).catch(() => {
        this.spuOptions = [];
      });
    },
    addSku() {
      this.skuForm = emptySkuForm();
      this.show = true;
      this.$nextTick(() => {
        if (this.$refs.skuForm) {
          this.$refs.skuForm.resetValidation();
        }
      });
    },
    editSku(sku) {
      this.skuForm = {
        id: sku.id,
        spuId: sku.spuId,
        homeArea: this.homeArea(sku) || "vegetables",
        title: sku.title,
        priceYuan: this.formatPrice(sku.price),
        stock: sku.stock || 0,
        enable: sku.enable,
        imageList: sku.images ? sku.images.split(",").filter(Boolean) : []
      };
      this.show = true;
    },
    uploadSkuImage(event) {
      const file = event.target.files && event.target.files[0];
      event.target.value = "";
      if (!file) {
        return;
      }
      const formData = new FormData();
      formData.append("file", file);
      this.$http.post("/item/sku/image", formData, {
        headers: { "Content-Type": "multipart/form-data" }
      }).then(resp => {
        this.skuForm.imageList.push(resp.data);
        this.$message.success("图片上传成功");
      }).catch(() => {
        this.$message.error("图片上传失败");
      });
    },
    removeImage(index) {
      this.skuForm.imageList.splice(index, 1);
    },
    saveSku() {
      if (this.$refs.skuForm && !this.$refs.skuForm.validate()) {
        return;
      }
      const data = {
        id: this.skuForm.id,
        spuId: this.skuForm.spuId,
        title: this.skuForm.title,
        price: Math.round(Number(this.skuForm.priceYuan || 0)),
        stock: Number(this.skuForm.stock || 0),
        enable: this.skuForm.enable,
        images: (this.skuForm.imageList || []).join(","),
        indexes: "0",
        ownSpec: JSON.stringify({ _homeArea: this.skuForm.homeArea })
      };
      const request = data.id ? this.$http.put("/item/sku", data) : this.$http.post("/item/sku", data);
      request.then(() => {
        this.$message.success("保存成功");
        this.show = false;
        this.getDataFromServer();
      }).catch(() => {
        this.$message.error("保存失败");
      });
    },
    updateEnable(sku, enable) {
      this.$http.put("/item/sku/" + sku.id + "/enable/" + enable).then(() => {
        this.$message.success(enable ? "启用成功" : "停用成功");
        this.getDataFromServer();
      }).catch(() => {
        this.$message.error("操作失败");
      });
    },
    deleteSku(sku) {
      this.$message.confirm("确认停用该前台商品吗？")
        .then(() => this.$http.delete("/item/sku/" + sku.id))
        .then(() => {
          this.$message.success("停用成功");
          this.getDataFromServer();
        })
        .catch(() => {});
    },
    homeArea(sku) {
      try {
        const ownSpec = JSON.parse(sku.ownSpec || "{}");
        return ownSpec._homeArea || "";
      } catch (e) {
        return "";
      }
    },
    areaName(sku) {
      const area = this.areaOptions.find(item => item.value === this.homeArea(sku));
      return area ? area.text : "-";
    },
    linkedSpuName(spuId) {
      const spu = this.spuOptions.find(item => Number(item.id) === Number(spuId));
      return spu ? spu.title : "-";
    },
    firstImage(images) {
      if (!images) {
        return "";
      }
      const url = String(images).split(",").filter(Boolean)[0] || "";
      return this.normalizeImageUrl(url);
    },
    normalizeImageUrl(url) {
      if (!url) {
        return "";
      }
      if (url.indexOf("http://localhost:10010") === 0 || url.indexOf("http://127.0.0.1:10010") === 0) {
        return url.replace(/^http:\/\/(localhost|127\.0\.0\.1):10010/, config.url);
      }
      if (url.indexOf("/api/") === 0) {
        return config.url + url;
      }
      if (url.indexOf("/sku/images/") === 0) {
        return config.api + "/item" + url;
      }
      return url;
    },
    formatPrice(price) {
      return Number(price || 0).toFixed(2).replace(/\.00$/, "");
    }
  }
};
</script>

<style scoped>
.sku-img {
  width: 58px;
  height: 58px;
  object-fit: cover;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-item,
.upload-box {
  width: 96px;
  height: 96px;
  border: 1px dashed #bbb;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image {
  position: absolute;
  right: 0;
  top: 0;
  background: rgba(255, 255, 255, 0.8);
}

.upload-box {
  cursor: pointer;
}

.upload-box input {
  display: none;
}
</style>
