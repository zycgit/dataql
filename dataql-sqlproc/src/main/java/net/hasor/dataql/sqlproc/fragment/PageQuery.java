//    /** 执行 SQL */
//    protected <T> T executeSQL(boolean batch, String sourceName, String sqlString, ExecuteProxy execute, Map<String, Object> params, Page page) throws SQLException {
//        if (this.spiTrigger.hasSpi(FxSqlCheckChainSpi.class)) {
//            final FxSqlInfo fxSqlInfo = new FxSqlInfo(batch, sourceName, sqlString, paramArrays);
//            final AtomicBoolean doExit = new AtomicBoolean(false);
//            this.spiTrigger.chainSpi(FxSqlCheckChainSpi.class, (listener, lastResult) -> {
//                if (doExit.get()) {
//                    return lastResult;
//                }
//                int doCheck = listener.doCheck(fxSqlInfo);
//                if (doCheck == FxSqlCheckChainSpi.EXIT) {
//                    doExit.set(true);
//                }
//                return lastResult;
//            }, fxSqlInfo);
//            //
//            return sqlQuery.doQuery(fxSqlInfo.getQueryString(), fxSqlInfo.getQueryParams(), this.getJdbcTemplate(sourceName));
//        } else {
//            return sqlQuery.doQuery(sqlString, paramArrays, this.getJdbcTemplate(sourceName));
//        }
//    }
