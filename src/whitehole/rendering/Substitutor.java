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

package whitehole.rendering;

import whitehole.smg.object.MapPartObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.object.GravityObj;
import whitehole.smg.object.CutsceneObj;
import whitehole.smg.object.PositionObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.ChildObj;
import whitehole.smg.object.LevelObj;
import whitehole.smg.object.CameraObj;
import whitehole.smg.object.AbstractObj;
import whitehole.smg.object.SoundObj;
import whitehole.rendering.object.AreaRenderer.Shape;
import whitehole.rendering.object.*;
import whitehole.smg.StageArchive;
import whitehole.util.Color4;
import whitehole.util.Vector3;
import java.io.IOException;
import whitehole.Whitehole;


public class Substitutor {
    public static String substituteModelName(AbstractObj obj, String model) {
        switch (obj.name) {
            case "ArrowSwitchMulti": return "ArrowSwitch";
            case "AstroDomeBlueStar": return "GCaptureTarget";
            case "AttackRockFinal":
            case "AttackRockTutorial": return "AttackRock";
            case "BenefitItemInvincible": return "PowerUpInvincible";
            case "BenefitItemLifeUp": return "KinokoLifeUp";
            case "BenefitItemOneUp": return "KinokoOneUp";
            case "BigBubbleGenerator":
            case "BigObstructBubbleGenerator": return "AirBubbleGenerator";
            case "Bomb": return "BombHei";
            case "BombLauncher": return "BombHeiLauncher";
            case "BossKameck2": return "BossKameck";
            case "BreakableCageRotate": return "BreakableCage";
            case "ButlerExplain":
            case "ButlerMap": return "Butler";
            case "CoinReplica": return "Coin";
            case "Creeper": return "CreeperFlower";
            case "CutBushGroup": return "CutBush";
            case "DemoKoopaJrShip": return "KoopaJrShip";
            case "DharmaSambo": return "DharmaSamboParts";
            case "FireBallBeamKameck": return "Kameck";
            case "FirePressureRadiate": return "FirePressure";
            case "FishGroupA": return "FishA";
            case "FishGroupB": return "FishB";
            case "FishGroupC": return "FishC";
            case "FishGroupD": return "FishD";
            case "FishGroupE": return "FishE";
            case "FishGroupF": return "FishF";
            case "FlowerBlueGroup": return "FlowerBlue";
            case "FlowerGroup": return "Flower";
            case "GhostPlayer": return "GhostMario";
            case "GliBirdNpc": return "GliBird";
            case "GoldenTurtle": return "KouraShine";
            case "Hanachan": return "HanachanHead";
            case "HanachanBig": return "HanachanHeadBig";
            case "DrillBullet": return "Horino";
            case "InstantInferno": return "InfernoMario";
            case "JetTurtle": return "Koura";
            case "KameckKuriboMini":
            case "KameckMeramera": return "Kameck";
            case "Karikari": return "Karipon";
            case "KirairaRail": return "Kiraira";
            case "KoopaBattleMapCoinPlate": return "KoopaPlateCoin";
            case "KoopaBattleMapPlate": return "KoopaPlate";
            case "KoopaBattleMapStairturnAppear": return "KoopaBattleMapStairTurn";
            case "KoopaNpc": return "Koopa";
            case "KoopaStatueVomit": return "KoopaStatue";
            case "KoopaLv2":
            case "KoopaLv3":
            case "KoopaLv4": return "Koopa";
            case "LavaProminenceWithoutShadow": return "LavaProminence";
            case "SpinLeverSwitchForceAnim": return "SpinLeverSwitch";
            case "LuigiTalkNpc":
            case "LuigiIntrusively": return "LuigiNpc";
            case "MagicBell": return "Bell";
            case "MameMuimuiAttackMan": return "ScoreAttackMan";
            case "MeteorCannon":
            case "MeteorStrikeEnvironment": return "MeteorStrike";
            case "MiniKoopaBattleVs1Galaxy":
            case "MiniKoopaBattleVs2Galaxy":
            case "MiniKoopaBattleVs3Galaxy": return "MiniKoopaGalaxy";
            case "MorphItemCollectionBee": return "PowerUpBee";
            case "MorphItemCollectionCloud": return "PowerUpCloud";
            case "MorphItemCollectionDrill": return "ItemDrill";
            case "MorphItemCollectionFire": return "PowerUpFire";
            case "MorphItemCollectionHopper": return "PowerUpHopper";
            case "MorphItemCollectionTeresa": return "PowerUpTeresa";
            case "MorphItemCollectionRock": return "PowerUpRock";
            case "MorphItemNeoBee": return "PowerUpBee";
            case "MorphItemNeoFire": return "PowerUpFire";
            case "MorphItemNeoFoo": return "PowerUpFoo";
            case "MorphItemNeoHopper": return "PowerUpHopper";
            case "MorphItemNeoIce": return "PowerUpIce";
            case "MorphItemNeoTeresa": return "PowerUpTeresa";
            case "MorphItemRock": return "PowerUpRock";
            case "NoteFairy": return "Note";
            case "OnimasuPivot": return "Onimasu";
            case "PenguinSkater":
            case "PenguinStudent": return "Penguin";
            case "Plant": return "PlantSeed";
            case "PlayAttackMan": return "ScoreAttackMan";
            case "PrologueDirector": return "DemoLetter";
            case "PukupukuWaterSurface": return "Pukupuku";
            case "Rabbit": return "MoonRabbit";
            case "RockCreator": return "Rock";
            case "RunawayRabbitCollect": return "TrickRabbit";
            case "SeaGullGroup":
            case "SeaGullGroupMarioFace": return "Seagull";
            case "ShellfishBlueChip":
            case "ShellfishCoin":
            case "ShellfishKinokoOneUp":
            case "ShellfishYellowChip": return "Shellfish";
            case "SignBoardTamakoro": return "SignBoard";
            case "SkeletalFishBaby": return "SnakeFish";
            case "SpiderAttachPoint": return "SpiderThreadAttachPoint";
            case "SpiderCoin": return "Coin";
            case "SpinCloudItem":
            case "SpinCloudMarioItem": return "PowerUpCloud";
            case "ItemBlockSwitch":
            case "SplashCoinBlock":
            case "SplashPieceBlock":
            case "TimerCoinBlock":
            case "TimerPieceBlock": return "CoinBlock";
            case "SuperDreamer": return "HelperWitch";
            case "SuperSpinDriverGreen":
            case "SuperSpinDriverPink": return "SuperSpinDriver";
            case "SurpBeltConveyerExGalaxy":
            case "SurpCocoonExGalaxy":
            case "SurpCubeBubbleExLv2Galaxy":
            case "SurpFishTunnelGalaxy":
            case "SurpPeachCastleFinalGalaxy":
            case "SurpSnowCapsuleGalaxy":
            case "SurpSurfingLv2Galaxy":
            case "SurpTamakoroExLv2Galaxy":
            case "SurpTearDropGalaxy":
            case "SurpTeresaMario2DGalaxy":
            case "SurpTransformationExGalaxy": return "MiniSurprisedGalaxy";
            case "TalkSyati": return "Syati";
            case "TamakoroWithTutorial": return "Tamakoro";
            case "Teresa":
            case "TeresaChief": return "TeresaWater";
            case "TicoAstro":
            case "TicoDomeLecture": return "Tico";
            case "TicoFatCoin":
            case "TicoFatStarPiece":
            case "TicoGalaxy": return "TicoFat";
            case "TicoRail":
            case "TicoReading":
            case "TicoStarRing": return "Tico";
            case "TogepinAttackMan": return "ScoreAttackMan";
            case "Tongari2D": return "Tongari";
            case "TreasureBoxBlueChip":
            case "TreasureBoxCoin": return "TreasureBox";
            case "TreasureBoxCrackedAirBubble":
            case "TreasureBoxCrackedBlueChip":
            case "TreasureBoxCrackedCoin":
            case "TreasureBoxCrackedEmpty":
            case "TreasureBoxCrackedKinokoLifeUp":
            case "TreasureBoxCrackedKinokoOneUp":
            case "TreasureBoxCrackedPowerStar":
            case "TreasureBoxCrackedYellowChip": return "TreasureBoxCracked";
            case "TreasureBoxEmpty": return "TreasureBox";
            case "TreasureBoxGoldEmpty": return "TreasureBoxGold";
            case "TreasureBoxKinokoLifeUp":
            case "TreasureBoxKinokoOneUp":
            case "TreasureBoxYellowChip": return "TreasureBox";
            case "TrickRabbitFreeRun":
            case "TrickRabbitFreeRunCollect":
            case "TrickRabbitGhost": return "TrickRabbit";
            case "TripodBossCoin": return "Coin";
            case "TripodBossBottomKillerCannon":
            case "TripodBossKillerGenerator": return "TripodBossKillerCannon";
            case "TripodBossKinokoOneUp": return "KinokoOneUp";
            case "TripodBossUnderKillerCannon":
            case "TripodBossUpperKillerCannon": return "TripodBossKillerCannon";
            case "TubeSliderDamageObj": return "NeedlePlant";
            case "TubeSliderEnemy": return "Togezo";
            case "TubeSliderHana": return "HanachanHeadBig";
            case "TurtleBeamKameck": return "Kameck";
            case "TwoLegsBullet": return "Horino";
            case "WingBlockCoin":
            case "WingBlockStarPiece": return "WingBlock";
            case "YoshiCapture": return "YCaptureTarget";
            case "GreenStar": return "PowerStar";
        }
        return model;
    }
    
    public static String substituteObjectKey(AbstractObj obj, String objectkey) {
        switch (obj.name) {
            case "PlantA":
            case "PlantB":
            case "PlantC":
            case "PlantD": objectkey += String.format("_%1$d_%2$d", obj.data.get("ShapeModelNo"), obj.data.get("Obj_arg3")); break;
            case "MarinePlant": objectkey += String.format("_%1$d_%2$d", obj.data.get("ShapeModelNo"), obj.data.get("Obj_arg1")); break;
            case "Pole":
            case "PoleSquare":
            case "Pole2Way": objectkey += String.format("_%1$3f", obj.scale.y / obj.scale.x); break;
            case "BlackHole":
            case "BlackHoleCube": objectkey += String.format("_%1$d_%2$f_%3$f_%4$f", obj.data.getOrDefault("Obj_arg0", 1000), obj.scale.x, obj.scale.y, obj.scale.z); break;
            case "Kinopio": 
            case "KinopioAstro": objectkey += String.format("_%1$d", obj.data.get("Obj_arg1")); break; 
            case "UFOKinoko": objectkey += String.format("_%1$d", obj.data.get("Obj_arg0")); break;
            case "OtaKing": objectkey += String.format("_%1$d", obj.data.get("Obj_arg1")); break;
            case "Coin":
            case "PurpleCoin": objectkey += String.format("_%1$d", obj.data.get("Obj_arg7")); break;
            case "AstroDome":
            case "AstroDomeEntrance":
            case "AstroDomeSky":
            case "AstroStarPlate": objectkey += String.format("_%1$d", obj.data.get("Obj_arg0")); break;
            case "BreakableCage": objectkey += String.format("_%1$d", obj.data.get("Obj_arg7")); break;
        }
        
        if (Whitehole.getCurrentGameType() == 2) {
            if (obj instanceof AreaObj)
                objectkey = String.format("AreaShapeNo%1$d", obj.data.get("AreaShapeNo"));
            if (obj instanceof CameraObj)
                objectkey = String.format("CameraShapeNo%1$d", obj.data.get("AreaShapeNo"));
        }
        
        if (obj instanceof GravityObj)
            objectkey += String.format("_%1$f_%2$f_%3$f_%4$f", obj.data.get("Range"), obj.scale.x, obj.scale.y, obj.scale.z);
        
        return objectkey;
    }
        
    public static GLRenderer substituteRenderer(AbstractObj obj, GLRenderer.RenderInfo info) {
        try {
            // Specified object rendering
            if (obj instanceof LevelObj || obj instanceof MapPartObj) {
                switch (obj.name) {
                    // ShapeModel rendering
                    case "PlantA":
                    case "PlantB":
                    case "PlantC":
                    case "PlantD": return new ShapeRenderer(info, obj.name, (short)obj.data.get("ShapeModelNo"));
                    case "MarinePlant": return new ShapeRenderer(info, obj.name, (short)obj.data.get("ShapeModelNo"));
                    case "Pole":
                    case "PoleNoModel": return new PoleRenderer(info, obj.scale, "Pole");
                    case "PoleSquare":
                    case "PoleSquareNoModel":
                    case "Pole2Way": return new PoleRenderer(info, obj.scale, "PoleSquare");

                    // Flag rendering
                    case "Flag": return new BtiRenderer(info, "Flag", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                    case "FlagRaceA": return new BtiRenderer(info, "FlagRaceA", new Vector3(0f,75f,0f), new Vector3(0f,-75f,300f), true);
                    case "FlagSurfing": return new BtiRenderer(info, "FlagSurfing", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                    case "FlagTamakoro": return new BtiRenderer(info, "FlagTamakoro", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                    case "FlagPeachCastleA": return new BtiRenderer(info, "FlagPeachCastleA", new Vector3(0f,150f,0f), new  Vector3(0f,-150f,600f), true);
                    case "FlagPeachCastleB": return new BtiRenderer(info, "FlagPeachCastleB", new Vector3(0f,150f,0f), new  Vector3(0f,-150f,600f), true);
                    case "FlagPeachCastleC": return new BtiRenderer(info, "FlagPeachCastleC", new Vector3(0f,150f,0f), new  Vector3(0f,-150f,600f), true);
                    case "FlagKoopaA": return new BtiRenderer(info, "FlagKoopaA", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                    case "FlagKoopaB": return new BtiRenderer(info, "FlagKoopaB", new Vector3(0f,75f,0f), new Vector3(0f,-75f,600f), true);
                    case "FlagKoopaCastle": return new BtiRenderer(info, "FlagKoopaCastle", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);

                    // Comet Observatory objects
                    case "AstroStarPlate":
                    case "AstroDome":
                    case "AstroDomeEntrance": return new AstroRenderer(info, obj.name, (int)obj.data.get("Obj_arg0"));
                    case "AstroDomeSky": return new AstroSkyRenderer(info, obj.name, (int)obj.data.get("Obj_arg0"));

                    // Black holes
                    case "BlackHole": return new BlackHoleRenderer(info, (int) obj.data.get("Obj_arg0"), obj.scale, Shape.SPHERE,false);
                    case "BlackHoleCube": return new BlackHoleRenderer(info, (int) obj.data.get("Obj_arg0"), obj.scale, Shape.CENTEREDCUBE,true);

                    // Other
                    case "Coin":
                    case "PurpleCoin": return new ItemBubbleRenderer(info, obj.name, (int)obj.data.get("Obj_arg7"));
                    case "OtaKing": return new OtaKingRenderer(info, obj.name, (int)obj.data.get("Obj_arg1"));
                    case "Kinopio": 
                    case "KinopioAstro": return new KinopioRenderer(info, (int)obj.data.get("Obj_arg1"));
                    case "KinopioBank": return new KinopioRenderer(info, 1);
                    case "KinopioPostman": return new KinopioRenderer(info, 2);
                    case "UFOKinoko": return new UFOKinokoRenderer(info, (int)obj.data.get("Obj_arg0"));
                    case "PowerStarHalo": return new PowerStarHaloRenderer(info);
                    case "EarthenPipe":
                    case "EarthenPipeInWater": return new BmdRendererSingle(info, "EarthenPipe", new Vector3(0f,100f,0f), new Vector3());
                    case "InvisibleWall10x10": return new TransparentWallRenderer(1);
                    case "InvisibleWall10x20": return new TransparentWallRenderer(2);
                    
                    // Multi-model rendering
                    case "RedBlueTurnBlock": return new MultiRenderer(
                            new BmdRenderer(info,"RedBlueTurnBlock"),
                            new BmdRenderer(info,"RedBlueTurnBlockBase")
                    );
                    case "Patakuri": return new MultiRenderer(
                            new BmdRendererSingle(info, "Kuribo"),
                            new BmdRendererSingle(info, "PatakuriWing", new Vector3(0f,15f,-25f), new Vector3())
                    );
                    case "PatakuriBig": return new MultiRenderer(
                            new BmdRendererSingle(info, "KuriboChief"),
                            new BmdRendererSingle(info, "PatakuriWingBig", new Vector3(0f,750f,200f), new Vector3(0f,90f,0f))
                    );
                    case "Nyoropon": return new MultiRenderer(
                            new BmdRendererSingle(info, "NyoroponBody"),
                            new BmdRendererSingle(info, "NyoroponHead", new Vector3(0f,500f,0f), new Vector3(90f,0f,0f))
                    );
                    case "Grapyon": return new MultiRenderer(
                            new BmdRendererSingle(info, "GrapyonBody"),
                            new BmdRendererSingle(info, "GrapyonHead", new Vector3(0f,80f,0f), new Vector3(0f,0f,0f))
                    );
                    
                    case "StrayTico": return new MultiRenderer(
                            new BmdRendererSingle(info, "StrayTico"),
                            new BmdRendererSingle(info, "ItemBubble")
                    );
                    case "HammerHeadPackun": return new MultiRenderer(
                            new BmdRendererSingle(info, "PackunFlower"),
                            new BmdRendererSingle(info, "PackunLeaf")
                    );
                    case "HammerHeadPackunSpike": return new MultiRenderer(
                            new BmdRendererSingle(info, "PackunFlowerSpike"),
                            new BmdRendererSingle(info, "PackunLeafSpike")
                    );
                    case "CocoSambo": return new MultiRenderer(
                            new BmdRendererSingle(info, "CocoSamboBody"),
                            new BmdRendererSingle(info, "CocoSamboHead", new Vector3(0f,325f,0f), new Vector3())
                    );
                    case "Kiraira": return new MultiRenderer(
                            new BmdRendererSingle(info, "Kiraira", new Vector3(0f,50f,0f), new Vector3()),
                            new BmdRendererSingle(info, "KirairaChain", new Vector3(0f,-160f,0f), new Vector3()),
                            new BmdRendererSingle(info, "KirairaFixPointBottom", new Vector3(0f,-15f,0f), new Vector3())
                    );
                    case "Torpedo": return new MultiRenderer(
                            new BmdRendererSingle(info, "Torpedo"),
                            new BmdRendererSingle(info, "TorpedoPropeller")
                    );
                    case "BegomanSpike": return new MultiRenderer(
                            new BmdRendererSingle(info, "BegomanSpike"),
                            new BmdRendererSingle(info, "BegomanSpikeHead")
                    );
                    case "BegomanSpring":
                    case "BegomanSpringHide": return new MultiRenderer(
                            new BmdRendererSingle(info, "BegomanSpring"),
                            new BmdRendererSingle(info, "BegomanSpringHead")
                    );
                    case "JumpBeamer": return new MultiRenderer(
                            new BmdRendererSingle(info, "JumpBeamerBody"),
                            new BmdRendererSingle(info, "JumpBeamerHead")
                    );
                    case "JumpGuarder": return new MultiRenderer(
                            new BmdRendererSingle(info, "JumpGuarder"),
                            new BmdRendererSingle(info, "JumpGuarderHeader")
                    );
                    case "GliderBazooka":
                    case "GliderShooter":
                    case "KillerShooter": return new MultiRenderer(
                            new BmdRendererSingle(info, "MogucchiSpike"),
                            new BmdRendererSingle(info, "GliderBazooka")
                    );
                    case "WaterBazooka": return new MultiRenderer(
                            new BmdRendererSingle(info, "WaterBazooka"),
                            new BmdRendererSingle(info, "WaterBazookaCapsule", new Vector3(0f, 475f, 0f), new Vector3()),
                            new BmdRendererSingle(info, "MogucchiShooter", new Vector3(0f,-160f,0f), new Vector3())
                    );
                    case "ElectricBazooka": return new MultiRenderer(
                            new BmdRendererSingle(info, "ElectricBazooka"),
                            new BmdRendererSingle(info, "WaterBazookaCapsule", new Vector3(0f, 475f, 0f), new Vector3()),
                            new BmdRendererSingle(info, "MogucchiShooter", new Vector3(0f,-160f,0f), new Vector3())
                    );
                    case "DinoPackun":
                    case "DinoPackunVs1": return new MultiRenderer(
                            new BmdRendererSingle(info, "DinoPackun"),
                            new BmdRendererSingle(info, "DinoPackunTailBall", new Vector3(0f,150f,-750f), new Vector3(0f,90f,0f))
                    );
                    case "DinoPackunVs2": return new MultiRenderer(
                            new BmdRendererSingle(info, "DinoPackun2"),
                            new BmdRendererSingle(info, "DinoPackunTailBall", new Vector3(0f,150f,-750f), new Vector3(0f,90f,0f))
                    );
                    case "BossBegoman": return new MultiRenderer(
                            new BmdRendererSingle(info, "BossBegoman"),
                            new BmdRendererSingle(info, "BossBegomanHead")
                    );
                    case "BossJugem": return new MultiRenderer(
                            new BmdRendererSingle(info, "BossJugem"),
                            new BmdRendererSingle(info, "BossJugemCloud")
                    );
                    case "KoopaJrRobot": return new MultiRenderer(
                            new BmdRendererSingle(info, "KoopaJrRobot"),
                            new BmdRendererSingle(info, "KoopaJrRobotPod", new Vector3(0f,1000f,0f), new Vector3())
                    );
                    case "KoopaJrCastle": return new MultiRenderer(
                            new BmdRendererSingle(info, "KoopaJrCastleBody"),
                            new BmdRendererSingle(info, "KoopaJrCastleHead", new Vector3(0f,2750f,0f), new Vector3()),
                            new BmdRendererSingle(info, "KoopaJrCastleCapsule", new Vector3(0f,700f,0f), new Vector3())
                    );
                    case "OtaRockTank": return new MultiRenderer(
                            new BmdRendererSingle(info, "OtaRockTank"),
                            new BmdRendererSingle(info, "OtaRockChief", new Vector3(0f, 500f, 0f), new Vector3())
                    );
                    case "TombSpider": return new MultiRenderer(
                            new BmdRendererSingle(info, "TombSpider"),
                            new BmdRendererSingle(info, "TombSpiderPlanet")
                    );
                    case "SkeletalFishBoss": return new MultiRenderer(
                            new BmdRendererSingle(info, "SkeletalFishBoss"),
                            new BmdRendererSingle(info, "SkeletalFishBossHeadA")
                    );
                    case "ClipAreaBoxBottom": return new ClipAreaBoxRenderer();
                    case "ClipAreaBoxCenterHighModel": return new ClipAreaBoxRenderer();
                }
            }
            
            if (obj instanceof AreaObj || obj instanceof CameraObj) {
                if (Whitehole.getCurrentGameType() == 2) {
                    Shape shape;
                    switch((short) obj.data.get("AreaShapeNo")) {
                        case 0:
                        case 1: shape = Shape.CUBE; break;
                        case 2: shape = Shape.SPHERE; break;
                        case 3: shape = Shape.CYLINDER; break;
                        default: shape = Shape.UNDEFINED;
                    }

                    if (shape == Shape.UNDEFINED) {
                        if (obj instanceof AreaObj)
                            return new ColorCubeRenderer (100f, new Color4(1f, 0.5f, 0.5f), new Color4(0.3f, 1f, 1f), true);
                        if (obj instanceof CameraObj)
                            return new ColorCubeRenderer (100f, new Color4(0.3f, 0f, 1f), new Color4(0.8f, 0f, 0f), true);
                    } else {
                        if (obj instanceof AreaObj)
                            return new AreaRenderer(new Color4(0.3f, 1f, 1f), shape);
                        if (obj instanceof CameraObj)
                            return new AreaRenderer(new Color4(0.8f, 0f, 0f), shape);
                    }
                }
                else if (Whitehole.getCurrentGameType() == 1) {
                    switch(obj.name) {
                        // AreaObj cubic
                        case "AstroChangeStageCube":
                        case "AudioEffectCube":
                        case "BeeWallShortDistAreaCube":
                        case "BgmProhibitArea":
                        case "BigBubbleGoalAreaBox":
                        case "BigBubbleSwitchBox":
                        case "BindEndCube":
                        case "BlackHoleCube":
                        case "BloomCube":
                        case "BlueStarGuidanceCube":
                        case "DarkMatterCube":
                        case "DepthOfFieldCube":
                        case "ExtraWallCheckArea":
                        case "FallsCube":
                        case "ForbidJumpCube":
                        case "ForbidTriangleJumpCube":
                        case "ForbidWaterSearchCube":
                        case "ForceDashCube":
                        case "HazeCube":
                        case "HeavySteeringCube":
                        case "LensFlareArea":
                        case "MercatorCube":
                        case "MirrorAreaCube":
                        case "NonSleepCube":
                        case "PipeModeCube":
                        case "PlaneCircularModeCube":
                        case "PlaneCollisionCube":
                        case "PlanetModeCube":
                        case "PlayerSeCube":
                        case "QuakeEffectAreaCube":
                        case "RasterScrollCube":
                        case "ScreenBlurCube":
                        case "SimpleBloomCube":
                        case "SmokeEffectColorAreaCube":
                        case "SoundEmitterCube":
                        case "SpinGuidanceCube":
                        case "SunLightAreaBox":
                        case "TamakoroJumpGuidanceCube":
                        case "TamakoroMoveGuidanceCube":
                        case "TicoSeedGuidanceCube":
                        case "TripodBossStepStartArea": return new AreaRenderer(new Color4(0.3f, 1f, 1f), Shape.CUBE);

                        // AreaObj spherical
                        case "AreaMoveSphere":
                        case "AudioEffectSphere":
                        case "BigBubbleGoalAreaSphere":
                        case "BigBubbleSwitchSphere":
                        case "BloomSphere":
                        case "CelestrialSphere":
                        case "DepthOfFieldSphere":
                        case "PlayerSeSphere":
                        case "SimpleBloomSphere":
                        case "SoundEmitterSphere":
                        case "ScreenBlurSphere": return new AreaRenderer(new Color4(0.3f, 1f, 1f), Shape.SPHERE);

                        // AreaObj cylindrical
                        case "AstroOverlookAreaCylinder":
                        case "AudioEffectCylinder":
                        case "BigBubbleGoalAreaCylinder":
                        case "BigBubbleSwitchCylinder":
                        case "BloomCylinder":
                        case "DarkMatterCylinder":
                        case "DashChargeCylinder":
                        case "DepthOfFieldCylinder":
                        case "DodoryuClosedCylinder":
                        case "EffectCylinder":
                        case "ExtraWallCheckCylinder":
                        case "GlaringLightAreaCylinder":
                        case "PlayerSeCylinder":
                        case "ScreenBlurCylinder":
                        case "SimpleBloomCylinder":
                        case "TowerModeCylinder": return new AreaRenderer(new Color4(0.3f, 1f, 1f), Shape.CYLINDER);

                        // CubeCamera cubic
                        case "BigBubbleCameraAreaBox":
                        case "CubeCameraBox": return new AreaRenderer(new Color4(0.8f, 0f, 0f), Shape.CUBE);

                        // CubeCamera spherical
                        case "BigBubbleCameraAreaSphere":
                        case "CameraRepulsiveSphere":
                        case "CubeCameraBowl":
                        case "CubeCameraSphere": return new AreaRenderer(new Color4(0.8f, 0f, 0f), Shape.SPHERE);

                        // CubeCamera cylindrical
                        case "BigBubbleCameraAreaCylinder":
                        case "CameraRepulsiveCylinder":
                        case "CubeCameraCylinder": return new AreaRenderer(new Color4(0.8f, 0f, 0f), Shape.CYLINDER);
                        
                        //CubeDeath cubic
                        case "DeathCube": return new AreaRenderer(new Color4(0.5f, 0f, 0f), Shape.CUBE);
                        
                        //CubeDeath spherical
                        case "DeathSphere": return new AreaRenderer(new Color4(0.5f, 0f, 0f), Shape.SPHERE);
                        
                        //CubeDeath cylindrical
                        case "DeathCylinder": return new AreaRenderer(new Color4(0.5f, 0f, 0f), Shape.CYLINDER);
                        
                        //CubeRestart cubic
                        case "RestartCube": return new AreaRenderer(new Color4(1f, 0.937254901961f, 0f), Shape.CUBE);
                        
                        //CubeWater cubic
                        case "WaterCube": return new AreaRenderer(new Color4(0f, 0f, 1f), Shape.CUBE);
                        
                        //CubaWater cylindrical
                        case "WaterCylinder": return new AreaRenderer(new Color4(0f, 0f, 1f), Shape.CYLINDER);
                        
                        //CubeMessage cubic
                        case "MessageAreaCube": return new AreaRenderer(new Color4(0.5f, 1f, 0.5f), Shape.CUBE);
                        
                        //CubeMessage cylindrical
                        case "MessageAreaCylinder": return new AreaRenderer(new Color4(0.5f, 1f, 0.5f), Shape.CYLINDER);
                        
                        //CubeViewGroupCtrl cubic
                        case "ViewGroupCtrlCube": return new AreaRenderer(new Color4(1f, 0f, 1f), Shape.CUBE);
                        
                        //CubeSwitch cubic
                        case "SwitchCube": return new AreaRenderer(new Color4(0.3f, 0.3f, 0.3f), Shape.CUBE);
                        
                        //CubeSwitch spheric
                        case "SwitchSphere": return new AreaRenderer(new Color4(0.3f, 0.3f, 0.3f), Shape.SPHERE);
                        
                        //CubeSwitch cylindrical
                        case "SwitchCylinder": return new AreaRenderer(new Color4(0.3f, 0.3f, 0.3f), Shape.CYLINDER);
                        
                        //CubeLightCtrl cubic
                        case "LightCtrlCube": return new AreaRenderer(new Color4(0, 0f, 0f), Shape.CUBE);
                        
                        //CubeLightCtrl 
                        case "LightCtrlCylinder": return new AreaRenderer(new Color4(0, 0f, 0f), Shape.CYLINDER);
                        
                        //CubePullBack cubic
                        case "PullBackCube": return new AreaRenderer(new Color4(1f, 0.4156862745098039f, 0f), Shape.CUBE);
                        
                        //CubePullBack cylindrical
                        case "PullBackCylinder": return new AreaRenderer(new Color4(1f, 0.4156862745098039f, 0f), Shape.CYLINDER);
                        
                        //CubeChangeBgm cubic
                        case "ChangeBgmCube": return new AreaRenderer(new Color4(0.7137254901960784f, 1f, 0f), Shape.CUBE);
                        
                        //CubeCollisionArea
                        case "CollisionArea": return new AreaRenderer(new Color4(1f, 1f, 1f), Shape.CUBE);
                    }
                }
            }
            
            // Planet rendering
            if (obj instanceof GravityObj) {
                switch(obj.name) {
                    // PlanetObj unfinished
                    case "GlobalDiskGravity":
                    case "GlobalDiskTorusGravity":
                    case "GlobalPlaneGravity":
                    case "GlobalSegmentGravity":
                    case "GlobalWireGravity": return new ColorCubeRenderer(100f, new Color4(1f, 1f, 1f), new Color4(0f,0.8f,0f), true);

                    // PlanetObj cubic
                    case "GlobalCubeGravity":
                    case "GlobalPlaneGravityInBox":
                    case "ZeroGravityBox":  return new GravityRenderer(obj.scale, (float) obj.data.get("Range"), Shape.CUBE);

                    // PlanetObj spherical
                    case "GlobalPointGravity":
                    case "ZeroGravitySphere":  return new GravityRenderer(obj.scale, (float) obj.data.get("Range"), Shape.SPHERE);

                    // PlanetObj cylindrical
                    case "GlobalBarrelGravity":
                    case "GlobalPlaneGravityInCylinder":
                    case "ZeroGravityCylinder": return new GravityRenderer(obj.scale, (float) obj.data.get("Range"), Shape.CYLINDER);

                    // PlanetObj cone
                    case "GlobalConeGravity": return new GravityRenderer(obj.scale, (float) obj.data.get("Range"), Shape.CONE);
                }
            }
            
            // Other object rendering
            if (obj.getClass() == ChildObj.class)
                return new ColorCubeRenderer (100f, new Color4(1f, 1f, 1f), new Color4(1f, 0.5f, 0.5f), true);
            
            if (obj.getClass() == CutsceneObj.class)
                return new ColorCubeRenderer (100f, new Color4(1f, 0.5f, 0.5f), new Color4(1.0f, 1.0f, 0.3f), true);
            
            if (obj.getClass() == PositionObj.class)
                return new ColorCubeRenderer (100f, new Color4(1f, 1f, 1f), new Color4(1f,0.5f,0f), true);
            
            if (obj.getClass() == SoundObj.class)
                return new ColorCubeRenderer (100f, new Color4(1f, 1f, 1f), new Color4(1f, 0.5f, 1f), true);
            
            if (obj.getClass() == DebugObj.class)
                return new ColorCubeRenderer (100f, new Color4(1f, 1f, 1f), new Color4(0.8f, 0.5f, 0.1f), true);
            
        } catch (IOException ex) {}
        
        return null;
    }
}
