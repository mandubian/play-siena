package models;

import play.modules.siena.Model;
import siena.Generator;
import siena.Id;
import siena.Table;
import siena.embed.EmbeddedMap;

@Table("otherid_models")
public class OtherIdModel extends Model{
    @Id(Generator.AUTO_INCREMENT)
    public Long myId;
    
    public String 	alpha;
    public short	beta;
    
    public String toString() {
    	return myId + " " + alpha + " " + beta;
    }
}
