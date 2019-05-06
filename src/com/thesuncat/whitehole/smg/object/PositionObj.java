/*
    © 2012 - 2019 - Whitehole Team

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package com.thesuncat.whitehole.smg.object;

import com.thesuncat.whitehole.Settings;
import com.thesuncat.whitehole.swing.PropertyGrid;
import com.thesuncat.whitehole.smg.Bcsv;
import com.thesuncat.whitehole.smg.ZoneArchive;
import com.thesuncat.whitehole.vectors.Vector3;
import java.util.List;
import java.util.ArrayList;

public class PositionObj extends AbstractObj {
    public PositionObj(ZoneArchive zone, String filepath, Bcsv.Entry entry) {
        this.type = "position";
        this.zone = zone;
        String[] stuff = filepath.split("/");
        directory = stuff[0];
        layer = stuff[1].toLowerCase();
        file = stuff[2];
        
        data = entry;
        
        name = (String)data.get("name");
        renderer = null;
        uniqueID = -1;
        
        loadDBInfo();
        
        position = new Vector3((float)data.get("pos_x"), (float)data.get("pos_y"), (float)data.get("pos_z"));
        rotation = new Vector3((float)data.get("dir_x"), (float)data.get("dir_y"), (float)data.get("dir_z"));
        scale = new Vector3(1,1,1);
    }
    
    public PositionObj(ZoneArchive zone, String filepath, int game, Vector3 pos) {
        this.zone = zone;
        String[] stuff = filepath.split("/");
        directory = stuff[0];
        layer = stuff[1].toLowerCase();
        file = stuff[2];
        
        data = new Bcsv.Entry();
        
        name = "GeneralPos";
        renderer = null;
        uniqueID = -1;
        
        loadDBInfo();
        
        position = pos;
        rotation = new Vector3(0f, 0f, 0f);
        scale = new Vector3(1f, 1f, 1f);
        
        data.put("name", name);
        data.put("pos_x", position.x); data.put("pos_y", position.y); data.put("pos_z", position.z);
        data.put("dir_x", rotation.x); data.put("dir_y", rotation.y); data.put("dir_z", rotation.z);
        data.put("PosName", "undefined");
        data.put("Obj_ID", (short)-1);
        if (ZoneArchive.game == 2)
            data.put("ChildObjId", (short)-1);
    }
    
    @Override
    public int save() {
        data.put("name", name);
        data.put("pos_x", position.x); data.put("pos_y", position.y); data.put("pos_z", position.z);
        data.put("dir_x", rotation.x); data.put("dir_y", rotation.y); data.put("dir_z", rotation.z);
        return 0;
    }

    @Override
    public void getProperties(PropertyGrid panel) {
        panel.addCategory("obj_rendering", Settings.japanese ? "レンダリング設定" : "Rendering");
        panel.addField("pos_x", getFieldName("pos_x"), "float", null, position.x, "Default");
        panel.addField("pos_y", getFieldName("pos_y"), "float", null, position.y, "Default");
        panel.addField("pos_z", getFieldName("pos_z"), "float", null, position.z, "Default");
        panel.addField("dir_x", getFieldName("dir_x"), "float", null, rotation.x, "Default");
        panel.addField("dir_y", getFieldName("dir_y"), "float", null, rotation.y, "Default");
        panel.addField("dir_z", getFieldName("dir_z"), "float", null, rotation.z, "Default");
        
        panel.addCategory("obj_position", "Position");
        panel.addField("PosName", "Position Name", "textlist", choicesPosName, data.get("PosName"), "Default"); 
        panel.addField("Obj_ID", "Object ID", "int", null, data.get("Obj_ID"), "Default");
        if (ZoneArchive.game == 1)
            panel.addField("ChildObjId", "Child ID", "int", null, data.get("ChildObjId"), "Default"); 
    }
    
    @Override
    public String toString() {
        String l = layer.equals("common") ? "Common" : "Layer" + layer.substring(5).toUpperCase();
        return dbInfo.name + " (" + data.get("PosName") + ") " + "[" + l + "]";
    }
    
    private static List<String> choicesPosName = new ArrayList() {{
        add("いいえ選択移動位置");
        add("アイテム惑星戻り");
        add("ウォーターバズーカマリオ位置");
        add("エピローグマリオ");
        add("カメラ基準点");
        add("ガード出現ポイント1");
        add("キノピオメッセンジャー");
        add("キングトッシンデモ開始位置");
        add("クッパ階段戦の砲弾出現");
        add("クッパＪｒシップ戦マリオ位置");
        add("グランドスター出現位置");
        add("グランドスター出現");
        add("グランドスター帰還リザルト");
        add("ゲート中心");
        add("ゲーム終了位置");
        add("ゲーム開始位置");
        add("コア中心");
        add("コンプリートエンディングデモ基準点");
        add("コーチチュートリアル位置");
        add("コーチレース終了後位置");
        add("コーチ２回目位置");
        add("ゴーストデモゴースト位置");
        add("ゴーストデモマリオ位置");
        add("ジュゲム突進点１");
        add("ジュゲム突進点２");
        add("スターゲットデモ座標");
        add("スタートカメラマリオ座標");
        add("スタート位置（サーフィン）");
        add("スピンドライバ初出基準点");
        add("スピンドライバ初出終了位置");
        add("タイムアタック前位置");
        add("タイムアタック後位置");
        add("ダウンデモ後（マリオ）");
        add("ダウンデモ");
        add("チュートリアル位置");
        add("デモ中心");
        add("デモ位置（クッパＪｒ登場デモ）");
        add("ドドリュウ再セット");
        add("ドドリュウ岩");
        add("ドーム中心");
        add("ノーマルエンディングデモ基準点");
        add("ハニークイーンとの会話位置");
        add("バッタンキング基準位置");
        add("バトラーデモ終了");
        add("バトラーマップレクチャー");
        add("バトルシップ・タイムアタック前位置");
        add("バトルシップ・タイムアタック後位置");
        add("パマタリアンハンターデモ用");
        add("パワーアップデモ（クッパ）");
        add("パワーアップデモ（マリオ）");
        add("パワーアップデモＬｖ２（クッパ）");
        add("パワーアップデモＬｖ２（マリオ）");
        add("パワーアップデモＬｖ３（クッパ）");
        add("パワーアップデモＬｖ３（マリオ）");
        add("ピーチャンレーサーレース終了後位置");
        add("ピーチャン位置[スター渡し]");
        add("ピーチ登場デモ後（マリオ）");
        add("ピーチ誘拐デモ基準点");
        add("ピーチ誘拐デモ終了位置");
        add("プレイヤーデモ位置（ベビーディノパックン戦）");
        add("プレイヤーデモ位置（ベリードラゴン戦）");
        add("プレイヤー一時退避");
        add("ベビチコ出会い点");
        add("ベビーディノパックンデモ位置");
        add("ペンギン移動後");
        add("ボスジュゲムダウンデモ位置");
        add("ボスジュゲムデモ位置");
        add("ボスブッスン位置");
        add("ボス戦デモ開始位置");
        add("ポルタデモプレイヤー位置");
        add("ポルタ開始デモプレイヤー位置");
        add("マイスター位置[グランドスター帰還後]");
        add("マイスター位置[デフォルト]");
        add("マイスター位置[変化前]");
        add("マイスター位置[変化後]");
        add("マイスター位置[帰還後アイテム惑星]");
        add("マイスター位置[NPC紹介]");
        add("マリオイベント会話２");
        add("マリオイベント会話３");
        add("マリオイベント会話");
        add("マリオデモ位置");
        add("マリオボスべー対決");
        add("マリオ位置");
        add("マリオ位置00空01");
        add("マリオ位置01空00");
        add("マリオ位置02空03");
        add("マリオ位置03空00");
        add("マリオ位置04空03");
        add("マリオ位置05空02");
        add("マリオ位置06空02");
        add("マリオ位置07空04");
        add("マリオ位置08空01");
        add("マリオ位置[でしゃばりルイージ]");
        add("マリオ位置[アイテム惑星移動後]");
        add("マリオ位置[グランドスター帰還後]");
        add("マリオ位置[デモ中]");
        add("マリオ位置[デモ後]");
        add("マリオ位置[ハラペコスターピースチコ]");
        add("マリオ位置[ファイルセレクトから]");
        add("マリオ位置[ワールドマップから]");
        add("マリオ位置[再スタート]");
        add("マリオ位置[変化前]");
        add("マリオ位置[変化後]");
        add("マリオ位置[帰還後アイテム惑星]");
        add("マリオ位置[帰還後位置上]");
        add("マリオ位置[帰還]");
        add("マリオ位置[郵便屋さんイベント]");
        add("マリオ位置[NPC紹介]");
        add("マリオ再セット位置");
        add("マリオ再セット位置1");
        add("マリオ再セット位置2");
        add("マリオ再セット");
        add("マリオ再セット1");
        add("マリオ再セット2");
        add("マリオ再セット3");
        add("マリオ再セット4");
        add("マリオ最終デモ終了後位置");
        add("マリオ最終戦開始位置");
        add("マリオ移動後");
        add("マリオ顔惑星煙突出口");
        add("メラキンデモ後セット位置");
        add("メラキンマリオ再セット位置");
        add("モンテ投げターゲット位置");
        add("ヨッシー出会いデモ基準点");
        add("ヨッシー出会いデモ後マリオ位置");
        add("リスタート");
        add("ルイージ帰還");
        add("レース終了後位置テレサ");
        add("レース終了後位置");
        add("レース開始時マリオ位置");
        add("ロゼッタ状況説明マリオ");
        add("ワープ位置（サーフィン）");
        add("二脚ボス爆発デモ後座標");
        add("合体ブロック故郷点");
        add("図書室中心");
        add("子連れカメムシデモ後ポイント");
        add("惑星中心");
        add("惑星Ｌｖ２");
        add("惑星Ｌｖ３");
        add("戦闘開始（クッパ）");
        add("戦闘開始（マリオ）");
        add("朗読デモ終了");
        add("未入力");
        add("爆破デモ後マリオ");
        add("着弾点0");
        add("着弾点1");
        add("着弾点2");
        add("着弾点3");
        add("着弾点4");
        add("着弾点5");
        add("着弾点6");
        add("着弾点7");
        add("着弾点8");
        add("着弾点9");
        add("着弾点10");
        add("絵本移動ポイント000");
        add("絵本移動ポイント001");
        add("絵本移動ポイント002");
        add("落下点1");
        add("落下点2");
        add("落下点3");
        add("落下点4");
        add("落下点5");
        add("負け時マリオ位置");
        add("開始デモ");
        add("開始マリオ");
        add("階段の戦い０（クッパ）");
        add("階段の戦い１（クッパ）");
        add("階段の戦い２（クッパ）");
        add("隠れ位置");
        add("Ｌｖ１終了（クッパ）");
        add("Ｌｖ１終了（マリオ）");
        add("Ｌｖ１開始（クッパ）");
        add("Ｌｖ１開始（マリオ）");
        add("Ｌｖ２終了（クッパ）");
        add("Ｌｖ２終了（マリオ）");
        add("Ｌｖ２開始（クッパ）");
        add("Ｌｖ２開始（マリオ）");
        add("Ｌｖ３内側（クッパ）");
        add("Ｌｖ３内側（マリオ）");
        add("Ｌｖ３外側（クッパ）");
        add("Ｌｖ３外側（マリオ）");
        add("MarioDemoPos");
        add("MarioDemoPos2");
        add("MarioDemoPos3");
        add("MarioDemoPos4");
        add("TicoDemoPos1");
        add("TicoDemoPos2");
        add("TicoDemoPos3");
    }};
}