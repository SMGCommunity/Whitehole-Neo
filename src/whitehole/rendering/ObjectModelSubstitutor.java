/*
    Copyright 2012 The Whitehole team

    This file is part of Whitehole.

    Whitehole is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version.

    Whitehole is distributed in the hope that it will be useful, but WITHOUT ANY 
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
    FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along 
    with Whitehole. If not, see http://www.gnu.org/licenses/.
*/

package whitehole.rendering;

import whitehole.rendering.objRenderer.UFOKinoko;
import whitehole.rendering.objRenderer.Kinopio;
import whitehole.rendering.objRenderer.AstroPart;
import whitehole.rendering.objRenderer.PowerStar;
import whitehole.rendering.objRenderer.AstroSky;
import whitehole.rendering.objRenderer.Pole;
import whitehole.rendering.objRenderer.PoleSquare;
import java.io.IOException;
import whitehole.smg.object.PlanetObj;
import whitehole.smg.object.AreaObj;
import whitehole.smg.LevelObject;
import whitehole.smg.object.CameraCubeObj;
import whitehole.smg.object.ChangeObj;
import whitehole.smg.object.ChildObj;
import whitehole.smg.object.DemoObj;
import whitehole.smg.object.DebugObj;
import whitehole.smg.object.SoundObj;
import whitehole.smg.object.GeneralPosObj;
import whitehole.vectors.Color4;
import whitehole.vectors.Vector3;

public class ObjectModelSubstitutor 
{
    public static String substituteModelName(LevelObject obj, String modelname)
    {
        switch (obj.name)
        {
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
            case "ButlerExplain":
            case "ButlerMap": return "Butler";
            case "CoinReplica": return "Coin";
            case "Creeper": return "CreeperFlower";
            case "CutBushGroup": return "CutBush";
            case "DemoKoopaJrShip": return "KoopaJrShip";
            case "DinoPackunVs1": return "DinoPackun";
            case "DinoPackunVs2": return "DinoPackun2";
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
            case "Horino": return "DrillBullet";
            case "InstantInferno": return "InfernoMario";
            case "ItemBlockSwitch": return "CoinBlock";
            case "JetTurtle": return "Koura";
            case "KameckKuriboMini":
            case "KameckMeramera": return "Kameck";
            case "Karikari": return "Karipon";
            case "KinopioBank":
            case "KinopioPostman": return "Kinopio";
            case "KirairaRail": return "Kiraira";
            case "KoopaBattleMapCoinPlate": return "KoopaPlateCoin";
            case "KoopaBattleMapPlate": return "KoopaPlate";
            case "KoopaBattleMapStairturnAppear": return "KoopaBattleMapStairTurn";
            case "KoopaNpc": return "Koopa";
            case "KoopaStatueVomit": return "KoopaStatue";
            case "KoopaLv2":
            case "KoopaLv3":
            case "KoopaLv4":
            case "KoopaVs1":
            case "KoopaVs2":
            case "KoopaVs3": return "Koopa";
            case "LavaProminenceWithoutShadow": return "LavaProminence";
            case "LuigiIntrusively": return "Luigi";
            case "MagicBell": return "Bell";
            case "MameMuimuiAttackMan":
            case "MameMuimuiScorer":
            case "MameMuimuiScorerLv2": return "ScoreAttackMan";
            case "MeteorCannon":
            case "MeteorStrikeEnvironment": return "MeteorStrike";
            case "MiniKoopaBattleVs1Galaxy":
            case "MiniKoopaBattleVs2Galaxy":
            case "MiniKoopaBattleVs3Galaxy": return "MiniKoopaGalaxy";
            case "MorphItemNeoBee": return "PowerUpBee";
            case "MorphItemNeoFire": return "PowerUpFire";
            case "MorphItemNeoFoo": return "PowerUpFoo";
            case "MorphItemNeoHopper": return "PowerUpHopper";
            case "MorphItemNeoIce": return "PowerUpIce";
            case "MorphItemNeoTeresa": return "PowerUpTeresa";
            case "MorphItemRock": return "PowerUpRock";
            case "NoteFairy": return "Note";
            case "OnimasuPivot": return "Onimasu";
            case "PenguinRacer":
            case "PenguinRacerLeader":
            case "PenguinSkater":
            case "PenguinStudent": return "Penguin";
            case "PlayAttackMan": return "ScoreAttackMan";
            case "PrologueDirector": return "DemoLetter";
            case "PukupukuWaterSurface": return "Pukupuku";
            case "Rabbit": return "MoonRabbit";
            case "RockCreator": return "Rock";
            case "RunawayRabbitCollect": return "TrickRabbit";
            case "SeaGullGroup":
            case "SeaGullGroupMarioFace": return "SeaGull";
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
            case "SplashCoinBlock":
            case "SplashPieceBlock": return "CoinBlock";
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
            case "TeresaChief":
            case "TeresaWater": return "Teresa";
            case "TicoAstro":
            case "TicoDomeLecture": return "Tico";
            case "TicoFatCoin":
            case "TicoFatStarPiece":
            case "TicoGalaxy": return "TicoFat";
            case "TicoRail":
            case "TicoReading":
            case "TicoStarRing": return "Tico";
            case "TimerCoinBlock":
            case "TimerPieceBlock": return "CoinBlock";
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
            case "TripodBossBottomKillerCannon":
            case "TripodBossKillerGenerator": return "TripodBossKillerCannon";
            case "TripodBossKinokoOneUp": return "KinokoOneUp";
            case "TripodBossUnderKillerCannon":
            case "TripodBossUpperKillerCannon": return "TripodBossKillerCannon";
            case "TubeSliderDamageObj": return "NeedlePlant";
            case "TubeSliderEnemy": return "Togezo";
            case "TubeSliderHana": return "HanachanHeadBig";
            case "TurtleBeamKameck": return "Kameck";
            case "WaterSphere": return "VolumeSphere";
            case "WingBlockCoin":
            case "WingBlockStarPiece": return "WingBlock";
            case "YoshiCapture": return "YCaptureTarget";
        }
        return modelname;
    }
    
    public static String substituteObjectKey(LevelObject obj, String objectkey)
    {
        switch (obj.name)
        {
            case "Pole":
            case "PoleSquare": objectkey += String.format("_%1$3f", obj.scale.y / obj.scale.x); break;
            case "Kinopio": 
            case "KinopioAstro": objectkey = String.format("object_Kinopio_%1$d", obj.data.get("Obj_arg1")); break; 
            case "UFOKinoko": objectkey = String.format("object_UFOKinoko_%1$d", obj.data.get("Obj_arg0")); break;
            case "AstroDome":
            case "AstroDomeEntrance":
            case "AstroDomeSky":
            case "AstroStarPlate": objectkey += String.format("_%1$d", obj.data.get("Obj_arg0")); break;
        }
        return objectkey;
    }
    
    public static GLRenderer substituteRenderer(LevelObject obj, GLRenderer.RenderInfo info)
    {
        try
        {
            if (obj.getClass() == AreaObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 0.5f, 0.5f, 1f), new Color4(0.3f, 1f, 1f, 1f), true);
            
            if (obj.getClass() == CameraCubeObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(0.3f, 0f, 1f, 1f), new Color4(0.8f, 0f, 0f, 1f), true);
            
            if (obj.getClass() == ChildObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 1f, 1f, 1f), new Color4(1f, 0.5f, 0.5f, 0f), true);                         
            
            if (obj.getClass() == PlanetObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 0.5f, 0.5f, 1f), new Color4(0.8f, 0f, 0f, 1f), true);
            
            if (obj.getClass() == DemoObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 0.5f, 0.5f, 1f), new Color4(1.0f, 1.0f, 0.3f, 1f), true);
            
            if (obj.getClass() == GeneralPosObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 1f, 1f, 1f), new Color4(1f,0.5f,0f,1f), true);
            
            if (obj.getClass() == SoundObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 1f, 1f, 1f), new Color4(1f, 0.5f, 1f,1f), true);   
            
            if (obj.getClass() == ChangeObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 1f, 1f, 1f), new Color4(0f,0.8f,0f,1f), true); 
            
            if (obj.getClass() == DebugObj.class)
                return new ColorCubeRenderer
                (100f, new Color4(1f, 1f, 1f, 1f), new Color4(0.8f, 0.5f, 0.1f, 1f), true);       
            
            switch (obj.name)
            {
                case "Patakuri": return new DoubleBmdRenderer(info, "Kuribo", new Vector3(), "PatakuriWing", new Vector3(0f,15f,-25f));               

                case "Kinopio": 
                case "KinopioAstro": return new Kinopio(info, (int)obj.data.get("Obj_arg1"));
                case "UFOKinoko": return new UFOKinoko(info, (int)obj.data.get("Obj_arg0"));
                case "PowerStar": return new PowerStar(info, null);
                case "GreenStar": return new PowerStar(info, "GreenStar");
                                      
                case "Pole": return new Pole(info, obj.scale);
                case "PoleSquare": return new PoleSquare(info, obj.scale);

                case "Flag": return new BtiRenderer(info, "Flag", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                case "FlagTamakoro": return new BtiRenderer(info, "FlagTamakoro", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                case "FlagSurfing": return new BtiRenderer(info, "FlagSurfing", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                case "FlagRaceA": return new BtiRenderer(info, "FlagRaceA", new Vector3(0f,75f,0f), new Vector3(0f,-75f,300f), true);
                case "FlagKoopaCastle": return new BtiRenderer(info, "FlagKoopaCastle", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                case "FlagKoopaA": return new BtiRenderer(info, "FlagKoopaA", new Vector3(0f,150f,0f), new Vector3(0f,-150f,600f), true);
                case "FlagKoopaB": return new BtiRenderer(info, "FlagKoopaB", new Vector3(0f,75f,0f), new Vector3(0f,-75f,600f), true);
                case "FlagPeachCastleA": return new BtiRenderer(info, "FlagPeachCastleA", new Vector3(0f,150f,0f), new  Vector3(0f,-150f,600f), true);     
                case "FlagPeachCastleB": return new BtiRenderer(info, "FlagPeachCastleB", new Vector3(0f,150f,0f), new  Vector3(0f,-150f,600f), true);
                case "FlagPeachCastleC": return new BtiRenderer(info, "FlagPeachCastleC", new Vector3(0f,150f,0f), new  Vector3(0f,-150f,600f), true);                    
                    
                case "AstroDome":
                case "AstroDomeEntrance":
                case "AstroStarPlate":
                    return new AstroPart(info, obj.name, (int)obj.data.get("Obj_arg0"));
                case "AstroDomeSky":
                    return new AstroSky(info, obj.name, (int)obj.data.get("Obj_arg0"));
                    
                case "KillerShooter": return new DoubleBmdRenderer(info, "MogucchiSpike", new Vector3(), "GliderBazooka", new Vector3());
                case "GliderShooter": return new DoubleBmdRenderer(info, "MogucchiSpike", new Vector3(), "GliderBazooka", new Vector3());    
                case "GliderBazooka": return new DoubleBmdRenderer(info, "MogucchiSpike", new Vector3(), "GliderBazooka", new Vector3());    
                case "TombSpider": return new DoubleBmdRenderer(info, "TombSpider", new Vector3(), "TombSpiderPlanet", new Vector3());
                case "SkeletalFishBoss": return new DoubleBmdRenderer(info, "SkeletalFishBoss", new Vector3(), "SkeletalFishBossHeadA", new Vector3());
                case "RedBlueTurnBlock": return new DoubleBmdRenderer(info, "RedBlueTurnBlock", new Vector3(), "RedBlueTurnBlockBase", new Vector3());
                case "HammerHeadPackun": return new DoubleBmdRenderer(info, "PackunFlower", new Vector3(), "PackunLeaf", new Vector3());
                case "HammerHeadPackunSpike": return new DoubleBmdRenderer(info, "PackunFlowerSpike", new Vector3(), "PackunLeafSpike", new Vector3());
                case "BegomanSpike": return new DoubleBmdRenderer(info, "BegomanSpikeHead", new Vector3(), "BegomanSpike", new Vector3());
                case "BegomanSpring":
                case "BegomanSpringHide": return new DoubleBmdRenderer(info, "BegomanSpringHead", new Vector3(), "BegomanSpring", new Vector3());           
            }
        }
        catch (IOException ex) {}
        
        return null;
        
        
        
    }
}
