<template>
  <el-row type="flex" justify="space-around">
    <el-col :span="24">
      <el-card :padding="10">
        <div slot="header">
          <span class="panel-title">{{ $t('m.Rating_Ranklist') }}</span>
        </div>

        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-card shadow="never">
              <div class="my-rating-title">{{ $t('m.My_Practice_Rating') }}</div>
              <div class="my-rating-value">{{ myRating.practiceRating !== undefined && myRating.practiceRating !== null ? myRating.practiceRating : '--' }}</div>
              <div class="my-rating-sub">{{ $t('m.Solved') }}: {{ myRating.solvedCount !== undefined && myRating.solvedCount !== null ? myRating.solvedCount : '--' }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" style="margin-top: 12px" class="sm-no-margin-top">
            <el-card shadow="never">
              <div class="my-rating-title">{{ $t('m.My_Contest_Rating') }}</div>
              <div class="my-rating-value">{{ myRating.contestRating !== undefined && myRating.contestRating !== null ? myRating.contestRating : '--' }}</div>
              <div class="my-rating-sub">{{ $t('m.Contest_Count') }}: {{ myRating.contestCount !== undefined && myRating.contestCount !== null ? myRating.contestCount : '--' }}</div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>

      <el-card :padding="10" style="text-align: center;">
        <el-input
          :placeholder="$t('m.Rank_Search_Placeholder')"
          v-model="searchUser"
          @keyup.enter.native="getRankData(1)"
        >
          <el-button
            slot="append"
            icon="el-icon-search"
            class="search-btn"
            @click="getRankData(1)"
          ></el-button>
        </el-input>
      </el-card>

      <el-card :padding="10">
        <el-tabs v-model="activeTab" @tab-click="onTabChange">
          <el-tab-pane :label="$t('m.Practice_Rating_Rank')" name="practice"></el-tab-pane>
          <el-tab-pane :label="$t('m.Contest_Rating_Rank')" name="contest"></el-tab-pane>
        </el-tabs>

        <vxe-table
          :data="dataRank"
          :loading="loadingTable"
          align="center"
          highlight-hover-row
          :seq-config="{ seqMethod }"
          auto-resize
          style="font-weight: 500;"
        >
          <vxe-table-column type="seq" min-width="60"></vxe-table-column>
          <vxe-table-column
            field="username"
            :title="$t('m.User')"
            min-width="220"
            show-overflow
            align="left"
          >
            <template v-slot="{ row }">
              <avatar
                :username="row.username"
                :inline="true"
                :size="25"
                color="#FFF"
                :src="row.avatar"
                class="user-avatar"
              ></avatar>
              <a
                @click="getInfoByUsername(row.uid, row.username)"
                style="color:#2d8cf0;"
                >{{ row.username }}</a
              >
            </template>
          </vxe-table-column>
          <vxe-table-column field="nickname" :title="$t('m.Nickname')" width="180">
            <template v-slot="{ row }">
              <el-tag effect="plain" size="small" v-if="row.nickname">
                {{ row.nickname }}
              </el-tag>
            </template>
          </vxe-table-column>
          <vxe-table-column field="rating" :title="$t('m.Rating_Value')" min-width="120">
          </vxe-table-column>
          <vxe-table-column
            field="count"
            :title="activeTab === 'practice' ? $t('m.Solved') : $t('m.Contest_Count')"
            min-width="120"
          ></vxe-table-column>
        </vxe-table>

        <Pagination
          :total="total"
          :page-size.sync="limit"
          :current.sync="page"
          @on-change="getRankData"
          show-sizer
          @on-page-size-change="getRankData(1)"
          :layout="'prev, pager, next, sizes'"
        ></Pagination>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import api from '@/common/api';
import { mapGetters } from 'vuex';
import Avatar from 'vue-avatar';
const Pagination = () => import('@/components/oj/common/Pagination');

export default {
  name: 'rating-rank',
  components: {
    Pagination,
    Avatar,
  },
  data() {
    return {
      page: 1,
      limit: 30,
      total: 0,
      searchUser: null,
      loadingTable: false,
      activeTab: 'practice',
      dataRank: [],
      myRating: {},
    };
  },
  mounted() {
    this.tryLoadMyRating();
    this.getRankData(1);
  },
  methods: {
    onTabChange() {
      this.getRankData(1);
    },
    tryLoadMyRating() {
      if (!this.isAuthenticated) {
        this.myRating = {};
        return;
      }
      api
        .getMyRating()
        .then((res) => {
          this.myRating = res.data.data || {};
        })
        .catch(() => {
          this.myRating = {};
        });
    },
    getRankData(page) {
      this.loadingTable = true;
      this.page = page;
      const req =
        this.activeTab === 'contest'
          ? api.getContestRatingRank(page, this.limit, this.searchUser)
          : api.getPracticeRatingRank(page, this.limit, this.searchUser);

      req
        .then((res) => {
          this.loadingTable = false;
          this.total = res.data.data.total;
          this.dataRank = res.data.data.records;
        })
        .catch(() => {
          this.loadingTable = false;
        });
    },
    seqMethod({ rowIndex }) {
      return this.limit * (this.page - 1) + rowIndex + 1;
    },
    getInfoByUsername(uid, username) {
      this.$router.push({
        path: '/user-home',
        query: { uid, username },
      });
    },
  },
  computed: {
    ...mapGetters(['isAuthenticated']),
  },
  watch: {
    isAuthenticated() {
      this.tryLoadMyRating();
    },
  },
};
</script>

<style scoped>
.my-rating-title {
  font-weight: 600;
}
.my-rating-value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 6px;
}
.my-rating-sub {
  margin-top: 6px;
  color: #666;
}

@media screen and (min-width: 768px) {
  .el-input-group {
    width: 50%;
  }
  .sm-no-margin-top {
    margin-top: 0 !important;
  }
}
@media screen and (min-width: 1050px) {
  .el-input-group {
    width: 30%;
  }
}
</style>
