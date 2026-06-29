<template>
  <v-form v-model="valid"
          ref="myBrandForm">
    <v-text-field v-model="brand.name"
                  label="请输入品牌名称"
                  required
                  :rules="nameRules" />
    <v-text-field v-model="brand.letter"
                  label="请输入品牌首字母"
                  required
                  :rules="letterRules" />
    <v-cascader url="/item/category/list"
                multiple
                required
                v-model="brand.categories"
                label="请选择商品分类" />
    <v-layout row>
      <v-flex xs3>
        <span style="font-size: 16px; color: #444">品牌LOGO：</span>
      </v-flex>
      <v-flex>
        <div class="brand-image-picker">
          <img v-if="imagePreview"
               :src="imagePreview"
               class="brand-image-preview">
          <div v-else
               class="brand-image-empty">请选择图片</div>
          <input ref="brandImageInput"
                 type="file"
                 accept="image/jpeg,image/png,image/gif"
                 style="display:none"
                 @change="onImageChange">
          <v-btn small
                 color="primary"
                 @click="chooseImage">选择图片</v-btn>
          <v-btn small
                 flat
                 @click="clearImage">清除</v-btn>
        </div>
      </v-flex>
    </v-layout>
    <v-layout class="my-4" row>
      <v-spacer />
      <v-btn @click="submit"
             color="primary">提交</v-btn>
      <v-btn @click="clear">重置</v-btn>
    </v-layout>
  </v-form>
</template>

<script>
export default {
  name: "brand-form",
  props: {
    oldBrand: {
      type: Object
    },
    isEdit: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      valid: false, // 表单校验结果标记
      brand: {
        name: "", // 品牌名称
        letter: "", // 品牌首字母
        image: "", // 品牌logo
        categories: [] //品牌所属的商品分类数组
      },
      imageFile: null,
      imagePreview: "",
      nameRules: [
        v => !!v || "品牌名称不能为空",
        v => v.length> 1 || "品牌名称至少2位"
      ],
      letterRules: [
        v => !!v || "首字母不能为空",
        v => /^[a-zA-Z]{1}$/.test(v) || "品牌字母只能是1个字母"
      ]
    };
  },
  methods: {
    submit() {
      // 表单校验
      if (this.$refs.myBrandForm.validate()) {
        const { categories, letter } = this.brand;
        // 数据库中只要保存分类的id即可，因此我们对categories的值进行处理,只保留id，并转为字符串
        const cids = (categories || []).map(c => c.id).join(",");
        const data = new FormData();
        if (this.isEdit && this.brand.id) {
          data.append("id", this.brand.id);
        }
        data.append("name", this.brand.name || "");
        data.append("letter", letter.toUpperCase());
        data.append("image", this.brand.image || "");
        data.append("cids", cids);
        if (this.imageFile) {
          data.append("imageFile", this.imageFile);
        }
        this.$http({
          method: "post",
          url: "/item/brand",
          data
        })
          .then(() => {
            // 关闭窗口
            this.$emit("close");
            this.$message.success("保存成功！");
          })
          .catch(() => {
            this.$message.error("保存失败！");
        });
      }
    },
    chooseImage() {
      this.$refs.brandImageInput.click();
    },
    onImageChange(event) {
      const file = event.target.files[0];
      if (!file) {
        return;
      }
      if (!/^image\/(jpeg|png|gif)$/.test(file.type)) {
        this.$message.error("只能上传 JPG、PNG 或 GIF 图片");
        return;
      }
      this.imageFile = file;
      this.imagePreview = URL.createObjectURL(file);
    },
    clearImage() {
      this.imageFile = null;
      this.imagePreview = "";
      this.brand.image = "";
      if (this.$refs.brandImageInput) {
        this.$refs.brandImageInput.value = "";
      }
    },
    formatImageUrl(url) {
      if (!url) {
        return "";
      }
      return String(url).replace(
        "http://localhost:8082/images/",
        "http://localhost:10010/api/upload/images/"
      );
    },
    clear() {
      // 重置表单
      this.$refs.myBrandForm.reset();
      // 需要手动清空商品分类
      this.categories = [];
      this.clearImage();
    }
  },
  watch: {
    oldBrand: {
      // 监控oldBrand的变化
      handler(val) {
        if (val) {
          // 注意不要直接复制，否则这边的修改会影响到父组件的数据，copy属性即可
          this.brand = Object.deepCopy(val);
          this.imageFile = null;
          this.imagePreview = this.formatImageUrl(this.brand.image);
        } else {
          // 为空，初始化brand
          this.brand = {
            name: "",
            letter: "",
            image: "",
            categories: []
          };
          this.imageFile = null;
          this.imagePreview = "";
        }
      },
      deep: true
    }
  }
};
</script>

<style scoped>
  .brand-image-picker {
    display: flex;
    align-items: center;
  }

  .brand-image-preview,
  .brand-image-empty {
    width: 250px;
    height: 90px;
    margin-right: 12px;
    border: 1px dashed #d9d9d9;
    border-radius: 4px;
  }

  .brand-image-preview {
    object-fit: contain;
  }

  .brand-image-empty {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #999;
  }
</style>
